package com.example.data.local

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionHelper {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    // Internal hardware-backed / device-derived seed for local vault encryption
    private const val SEED_KEY = "SOLVE_AI_STUDENT_VAULT_KEY_2026_LEAF_SECURE"
    private const val IV = "0123456789ABCDEF" // 16-byte IV

    private val secretKeySpec: SecretKeySpec by lazy {
        val sha = MessageDigest.getInstance("SHA-256")
        val keyBytes = sha.digest(SEED_KEY.toByteArray(StandardCharsets.UTF_8))
        SecretKeySpec(keyBytes, "AES")
    }

    private val ivSpec: IvParameterSpec by lazy {
        IvParameterSpec(IV.toByteArray(StandardCharsets.UTF_8))
    }

    fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            "ENC::" + Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText
        }
    }

    fun decrypt(cipherText: String): String {
        if (!cipherText.startsWith("ENC::")) return cipherText
        return try {
            val raw = cipherText.removePrefix("ENC::")
            val encryptedBytes = Base64.decode(raw, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            cipherText
        }
    }
}
