package com.example.mobile_app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

private const val CURRENT_USER_PREFS = "current_user_prefs"
private const val KEY_USER_ID = "current_user_id"
private const val KEY_USER_NAME = "current_user_name"
private const val KEY_USER_EMAIL = "current_user_email"

class CurrentUserManager(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(CURRENT_USER_PREFS, Context.MODE_PRIVATE)

    fun saveCurrentUser(id: Long, name: String, email: String) {
        preferences.edit {
            putLong(KEY_USER_ID, id)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
        }
    }

    fun getCurrentUser(): CurrentUserInfo? {
        val id = preferences.getLong(KEY_USER_ID, -1L)
        val name = preferences.getString(KEY_USER_NAME, null)
        val email = preferences.getString(KEY_USER_EMAIL, null)
        return if (id > 0 && !name.isNullOrBlank() && !email.isNullOrBlank()) {
            CurrentUserInfo(id = id, name = name, email = email)
        } else {
            null
        }
    }

    fun clearCurrentUser() {
        preferences.edit {
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
        }
    }
}

data class CurrentUserInfo(
    val id: Long,
    val name: String,
    val email: String,
)
