package com.guptrix.app.crypto

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedPayload(
    val ciphertextBase64: String,
    val ivBase64: String
)

class CryptoManager {

    private val secureRandom = SecureRandom()

    fun generateKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance(AES_ALGORITHM)
        keyGen.init(KEY_SIZE_BITS, secureRandom)
        return keyGen.generateKey()
    }

    fun deriveKeyFromPassphrase(passphrase: String, salt: ByteArray): SecretKey {
        val keySpec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val derivedBytes = factory.generateSecret(keySpec).encoded
        return SecretKeySpec(derivedBytes, AES_ALGORITHM)
    }

    fun encrypt(plaintext: String, key: SecretKey): EncryptedPayload {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)

        val plaintextBytes = plaintext.toByteArray(StandardCharsets.UTF_8)
        val ciphertextBytes = cipher.doFinal(plaintextBytes)

        val encoder = Base64.getEncoder()
        return EncryptedPayload(
            ciphertextBase64 = encoder.encodeToString(ciphertextBytes),
            ivBase64 = encoder.encodeToString(iv)
        )
    }

    fun decrypt(payload: EncryptedPayload, key: SecretKey): String {
        val decoder = Base64.getDecoder()
        val iv = decoder.decode(payload.ivBase64)
        val ciphertextBytes = decoder.decode(payload.ciphertextBase64)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)

        val decryptedBytes = cipher.doFinal(ciphertextBytes)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    companion object {
        private const val AES_ALGORITHM = "AES"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val KEY_SIZE_BITS = 256
        private const val PBKDF2_ITERATIONS = 100_000
    }
}
