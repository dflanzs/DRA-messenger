@file:Suppress("unused")

package com.example.mobile_app.presentation.auth

import org.json.JSONObject
import retrofit2.HttpException

fun Throwable.toAuthUserMessage(defaultMessage: String): String {
    val httpException = this as? HttpException
    val rawBody = httpException
        ?.response()
        ?.errorBody()
        ?.string()
        ?.trim()
        .orEmpty()

    if (rawBody.isNotBlank()) {
        val parsed = runCatching {
            when {
                rawBody.startsWith("{") -> {
                    val json = JSONObject(rawBody)
                    json.optString("error").ifBlank { json.optString("message") }
                }
                rawBody.startsWith("\"") && rawBody.endsWith("\"") -> rawBody.trim('"')
                else -> rawBody
            }
        }.getOrNull()

        if (!parsed.isNullOrBlank()) return parsed
    }

    return message?.takeIf { it.isNotBlank() } ?: defaultMessage
}


