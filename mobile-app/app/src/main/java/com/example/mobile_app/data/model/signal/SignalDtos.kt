@file:Suppress("unused")

package com.example.mobile_app.data.model.signal

data class SignalOneTimePreKeyDto(
    val preKeyId: Int,
    val publicKeyB64: String,
)

data class SignalBootstrapRequestDto(
    val registrationId: Int,
    val identityKeyPublicB64: String,
    val signedPreKeyId: Int,
    val signedPreKeyPublicB64: String,
    val signedPreKeySignatureB64: String,
    val kyberPreKeyId: Int,
    val kyberPreKeyPublicB64: String,
    val kyberPreKeySignatureB64: String,
    val oneTimePreKeys: List<SignalOneTimePreKeyDto>,
)

data class SignalBootstrapResponseDto(
    val activeSignedPreKeyId: Int,
    val activeKyberPreKeyId: Int,
    val oneTimePreKeysStored: Int,
)

data class SignalBundleResponseDto(
    val registrationId: Int,
    val deviceId: Int,
    val identityKeyPublicB64: String,
    val signedPreKeyId: Int,
    val signedPreKeyPublicB64: String,
    val signedPreKeySignatureB64: String,
    val kyberPreKeyId: Int,
    val kyberPreKeyPublicB64: String,
    val kyberPreKeySignatureB64: String,
    val oneTimePreKeyId: Int?,
    val oneTimePreKeyPublicB64: String?,
)

data class SignalRefillRequestDto(
    val oneTimePreKeys: List<SignalOneTimePreKeyDto>,
)

data class SignalRefillResponseDto(
    val oneTimePreKeysStored: Int,
)

