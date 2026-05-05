package com.example.mobile_app.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mobile_app.BuildConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val PREFS_NAME = "network_prefs"
private const val KEY_BACKEND_URL_OVERRIDE = "backend_url_override"
private val BACKEND_URL_OVERRIDE_KEY = stringPreferencesKey(KEY_BACKEND_URL_OVERRIDE)

private val Context.networkDataStore by preferencesDataStore(name = PREFS_NAME)

object NetworkConfig {
    const val DEFAULT_BASE_URL: String = BuildConfig.BACKEND_BASE_URL

    fun resolveBaseUrl(context: Context? = null): String {
        if (context == null) return DEFAULT_BASE_URL

        val overrideBaseUrl = runBlocking {
            context.networkDataStore.data.first()[BACKEND_URL_OVERRIDE_KEY]
        }

        return overrideBaseUrl
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.ensureTrailingSlash()
            ?: DEFAULT_BASE_URL
    }

    fun saveBaseUrlOverride(context: Context, baseUrl: String) {
        runBlocking {
            context.networkDataStore.edit { preferences ->
                preferences[BACKEND_URL_OVERRIDE_KEY] = baseUrl.trim().ensureTrailingSlash()
            }
        }
    }

    fun clearBaseUrlOverride(context: Context) {
        runBlocking {
            context.networkDataStore.edit { preferences ->
                preferences.remove(BACKEND_URL_OVERRIDE_KEY)
            }
        }
    }

    private fun String.ensureTrailingSlash(): String =
        if (endsWith('/')) this else "$this/"
}

