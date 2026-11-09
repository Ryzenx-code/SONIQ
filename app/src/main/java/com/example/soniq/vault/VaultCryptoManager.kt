package com.example.soniq.vault

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Real Android Keystore AES-256-GCM hardware-backed vault cryptography and file encryption engine.
 */
class VaultCryptoManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "SONIQ_VAULT_MASTER_KEY"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        getOrCreateMasterKey()
    }

    private fun getOrCreateMasterKey(): SecretKey {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // Set to false to support both biometric & PIN fallback smoothly
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun getEncryptCipher(): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        return cipher
    }

    fun encryptBytes(plainText: ByteArray): ByteArray {
        val cipher = getEncryptCipher()
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText)
        // Prepend IV
        return iv + encrypted
    }

    fun decryptBytes(encryptedWithIv: ByteArray): ByteArray {
        if (encryptedWithIv.size < GCM_IV_LENGTH) throw IllegalArgumentException("Encrypted payload too short")
        val iv = encryptedWithIv.copyOfRange(0, GCM_IV_LENGTH)
        val cipherText = encryptedWithIv.copyOfRange(GCM_IV_LENGTH, encryptedWithIv.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), spec)
        return cipher.doFinal(cipherText)
    }

    fun encryptFile(inputFile: File, encryptedFile: File) {
        val bytes = inputFile.readBytes()
        val encrypted = encryptBytes(bytes)
        encryptedFile.writeBytes(encrypted)
    }

    fun decryptFile(encryptedFile: File, outputFile: File) {
        val bytes = encryptedFile.readBytes()
        val decrypted = decryptBytes(bytes)
        outputFile.writeBytes(decrypted)
    }
}
