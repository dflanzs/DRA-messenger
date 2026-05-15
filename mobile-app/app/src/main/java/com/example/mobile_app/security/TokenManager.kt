package com.example.mobile_app.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec.Builder as KeySpecBuilder
import org.json.JSONObject

class TokenManager(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val keyStore = java.security.KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun saveToken(token: String) {
        val encrypted = encrypt(token)
        preferences.edit { putString(KEY_TOKEN_ENCRYPTED, encrypted) }
    }

    fun getToken(): String? {
        val encrypted = preferences.getString(KEY_TOKEN_ENCRYPTED, null) ?: return null
        val token = runCatching { decrypt(encrypted) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: run {
                clearToken()
                return null
            }

        if (isJwtExpired(token)) {
            clearToken()
            return null
        }

        return token
    }

    fun clearToken() {
        preferences.edit { remove(KEY_TOKEN_ENCRYPTED) }
    }

    @Suppress("unused")
    fun hasToken(): Boolean = getToken().isNullOrBlank().not()

    private fun getOrCreateSecretKey(): SecretKey {
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? java.security.KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec: KeyGenParameterSpec = KeySpecBuilder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val payload = iv + encryptedBytes
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(base64Payload: String): String {
        val payload = Base64.decode(base64Payload, Base64.NO_WRAP)
        require(payload.size > IV_SIZE_BYTES) { "Invalid encrypted payload." }

        val iv = payload.copyOfRange(0, IV_SIZE_BYTES)
        val cipherText = payload.copyOfRange(IV_SIZE_BYTES, payload.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)

        return cipher.doFinal(cipherText).toString(Charsets.UTF_8)
    }

    private fun isJwtExpired(token: String): Boolean {
        val payload = decodeJwtPayload(token) ?: return true
        val expSeconds = runCatching {
            JSONObject(payload).optLong("exp", -1L)
        }.getOrDefault(-1L)

        return expSeconds <= 0L || System.currentTimeMillis() >= expSeconds * 1000L
    }

    private fun decodeJwtPayload(token: String): String? {
        val parts = token.split('.')
        if (parts.size < 2) return null

        val payload = parts[1]
        val padding = "=".repeat((4 - payload.length % 4) % 4)

        return runCatching {
            val decoded = Base64.decode(payload + padding, Base64.URL_SAFE or Base64.NO_WRAP)
            String(decoded, Charsets.UTF_8)
        }.getOrNull()
    }

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_TOKEN_ENCRYPTED = "jwt_token_encrypted"
        private const val KEY_ALIAS = "jwt_token_key_alias"

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val IV_SIZE_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}


