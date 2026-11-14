package com.guptrix.app.crypto

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

interface BiometricChecker {
    fun isHardwareAvailable(): Boolean
}

class DefaultBiometricChecker(private val context: Context?) : BiometricChecker {
    override fun isHardwareAvailable(): Boolean {
        if (context == null) return false
        return try {
            val biometricManager = BiometricManager.from(context)
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            ) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Exception) {
            false
        }
    }
}

class KeystoreVaultManager(
    private val prefs: SharedPreferences,
    private val biometricChecker: BiometricChecker = DefaultBiometricChecker(null),
    private val cryptoManager: CryptoManager = CryptoManager()
) {
    constructor(context: Context, cryptoManager: CryptoManager = CryptoManager()) : this(
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        biometricChecker = DefaultBiometricChecker(context),
        cryptoManager = cryptoManager
    )

    companion object {
        const val PREFS_NAME = "guptrix_vault_prefs"
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_PIN_SALT = "key_pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val BIOMETRIC_KEY_ALIAS = "GuptrixBiometricKey"
    }

    var isUnlocked: Boolean = false
        private set

    fun hasPinSet(): Boolean {
        return prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT)
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricHardwareAvailable(): Boolean {
        return biometricChecker.isHardwareAvailable()
    }

    fun setupPin(pin: String) {
        val salt = cryptoManager.generateSalt()
        val derivedKey = cryptoManager.deriveKeyFromPassphrase(pin, salt)
        val encoder = java.util.Base64.getEncoder()
        val hashBase64 = encoder.encodeToString(derivedKey.encoded)
        val saltBase64 = encoder.encodeToString(salt)

        prefs.edit()
            .putString(KEY_PIN_HASH, hashBase64)
            .putString(KEY_PIN_SALT, saltBase64)
            .apply()

        isUnlocked = true
    }

    fun verifyPin(inputPin: String): Boolean {
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val savedSalt = prefs.getString(KEY_PIN_SALT, null) ?: return false

        val decoder = java.util.Base64.getDecoder()
        val encoder = java.util.Base64.getEncoder()
        val salt = decoder.decode(savedSalt)
        val derivedKey = cryptoManager.deriveKeyFromPassphrase(inputPin, salt)
        val computedHash = encoder.encodeToString(derivedKey.encoded)

        val isValid = (savedHash == computedHash)
        if (isValid) {
            isUnlocked = true
        }
        return isValid
    }

    fun unlockByBiometrics() {
        isUnlocked = true
    }

    fun lockVault() {
        isUnlocked = false
    }

    fun removePin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .remove(KEY_BIOMETRIC_ENABLED)
            .apply()
        isUnlocked = false
    }

    fun getOrCreateBiometricCipher(): Cipher? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            if (!keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val builder = KeyGenParameterSpec.Builder(
                    BIOMETRIC_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false)

                keyGenerator.init(builder.build())
                keyGenerator.generateKey()
            }

            val secretKey = keyStore.getKey(BIOMETRIC_KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            cipher
        } catch (_: Exception) {
            null
        }
    }
}
