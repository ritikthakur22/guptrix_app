package com.guptrix.app.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.security.GeneralSecurityException

class CryptoManagerTest {

    private lateinit var cryptoManager: CryptoManager

    @Before
    fun setUp() {
        cryptoManager = CryptoManager()
    }

    @Test
    fun testEncryptionDecryptionRoundTrip() {
        val originalText = "Top secret Guptrix note with special chars: 🔒 12345 !@#$%"
        val key = cryptoManager.generateKey()

        val payload = cryptoManager.encrypt(originalText, key)
        assertNotNull(payload.ciphertextBase64)
        assertNotNull(payload.ivBase64)
        assertNotEquals(originalText, payload.ciphertextBase64)

        val decrypted = cryptoManager.decrypt(payload, key)
        assertEquals(originalText, decrypted)
    }

    @Test
    fun testDecryptionFailsWithWrongKey() {
        val originalText = "Sensitive vault data"
        val key1 = cryptoManager.generateKey()
        val key2 = cryptoManager.generateKey()

        val payload = cryptoManager.encrypt(originalText, key1)

        assertThrows(GeneralSecurityException::class.java) {
            cryptoManager.decrypt(payload, key2)
        }
    }

    @Test
    fun testDecryptionFailsWithCorruptedCiphertext() {
        val originalText = "Tamper check"
        val key = cryptoManager.generateKey()

        val payload = cryptoManager.encrypt(originalText, key)
        val corruptedPayload = EncryptedPayload(
            ciphertextBase64 = payload.ciphertextBase64.dropLast(4) + "AAAA",
            ivBase64 = payload.ivBase64
        )

        assertThrows(GeneralSecurityException::class.java) {
            cryptoManager.decrypt(corruptedPayload, key)
        }
    }

    @Test
    fun testKeyDerivationFromPassphrase() {
        val passphrase = "MasterVaultPassword#2026"
        val salt = "guptrix_test_salt_16b".toByteArray()

        val key1 = cryptoManager.deriveKeyFromPassphrase(passphrase, salt)
        val key2 = cryptoManager.deriveKeyFromPassphrase(passphrase, salt)

        assertEquals("AES", key1.algorithm)
        assertEquals(32, key1.encoded.size)
        assertEquals(key1, key2)

        val payload = cryptoManager.encrypt("Secret note", key1)
        val decrypted = cryptoManager.decrypt(payload, key2)
        assertEquals("Secret note", decrypted)
    }
}
