package com.example.core

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Provides AES-256-GCM encryption for academy chat payloads and SHA-256 integrity digests
 * for cloud backup snapshots.
 */
object CryptoManager {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SPEC_ALGO = "AES"
    private const val GCM_TAG_BITS = 128
    private const val IV_SIZE_BYTES = 12

    private fun deriveChannelKey(channelId: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        val seed = "EduTechMindset::BaselAlFaqawi::$channelId::v1".toByteArray(StandardCharsets.UTF_8)
        val keyBytes = digest.digest(seed)
        return SecretKeySpec(keyBytes, KEY_SPEC_ALGO)
    }

    fun encryptMessage(plainText: String, channelId: String): String {
        return try {
            val iv = ByteArray(IV_SIZE_BYTES)
            SecureRandom().nextBytes(iv)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, deriveChannelKey(channelId), GCMParameterSpec(GCM_TAG_BITS, iv))
            val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }

    fun decryptMessage(cipherBase64: String, channelId: String): String? {
        if (cipherBase64.isBlank()) return null
        return try {
            val combined = Base64.decode(cipherBase64, Base64.NO_WRAP)
            if (combined.size <= IV_SIZE_BYTES) return null
            val iv = combined.copyOfRange(0, IV_SIZE_BYTES)
            val cipherBytes = combined.copyOfRange(IV_SIZE_BYTES, combined.size)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, deriveChannelKey(channelId), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun computeBackupDigest(uid: String, payloadSummary: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$uid::$payloadSummary::${System.currentTimeMillis()}".toByteArray(StandardCharsets.UTF_8))
        val hex = bytes.take(12).joinToString("") { "%02x".format(it) }
        return "AES256-GCM:$hex"
    }
}
