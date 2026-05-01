package com.example.mobile_app.security

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveToken(token: String) {
        preferences.edit { putString(KEY_TOKEN, token) }
    }

    fun getToken(): String? = preferences.getString(KEY_TOKEN, null)

    fun clearToken() {
        preferences.edit { remove(KEY_TOKEN) }
    }

    fun hasToken(): Boolean = getToken().isNullOrBlank().not()

    companion object {
        private const val PREFS_NAME = "secure_auth_prefs"
        private const val KEY_TOKEN = "jwt_token"
    }
}


