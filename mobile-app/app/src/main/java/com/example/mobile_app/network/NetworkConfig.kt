package com.example.mobile_app.network

import android.content.Context
import com.example.mobile_app.BuildConfig

object NetworkConfig {
    val DEFAULT_BASE_URL: String = BuildConfig.BACKEND_BASE_URL

    @Suppress("UNUSED_PARAMETER")
    fun resolveBaseUrl(context: Context? = null): String = DEFAULT_BASE_URL
}

