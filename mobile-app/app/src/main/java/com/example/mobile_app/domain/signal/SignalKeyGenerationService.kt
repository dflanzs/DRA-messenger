@file:Suppress("unused")

package com.example.mobile_app.domain.signal

import com.example.mobile_app.data.model.signal.SignalBootstrapRequestDto
import com.example.mobile_app.data.model.signal.SignalOneTimePreKeyDto
import com.example.mobile_app.data.model.signal.SignalBootstrapResponseDto

class SignalKeyGenerationService(
    private val signalStore: SignalStore,
) {

    /**
     * Generate bootstrap request using stored keys if available, or create new ones.
     * This ensures keys are not regenerated on each app restart.
     */
    suspend fun generateAndBootstrap(): SignalBootstrapRequestDto {
        val material = signalStore.loadOrCreateBootstrapMaterial()

        return SignalBootstrapRequestDto(
            registrationId = material.registrationId,
            identityKeyPublicB64 = material.identityKeyPair.publicKey.serialize().b64(),
            signedPreKeyId = material.signedPreKeyRecord.id,
            signedPreKeyPublicB64 = material.signedPreKeyRecord.keyPair.publicKey.serialize().b64(),
            signedPreKeySignatureB64 = material.signedPreKeyRecord.signature.b64(),
            kyberPreKeyId = material.kyberPreKeyRecord.id,
            kyberPreKeyPublicB64 = material.kyberPreKeyRecord.keyPair.publicKey.serialize().b64(),
            kyberPreKeySignatureB64 = material.kyberPreKeyRecord.signature.b64(),
            oneTimePreKeys = material.oneTimePreKeyRecords.map { preKeyRecord ->
                SignalOneTimePreKeyDto(
                    preKeyId = preKeyRecord.id,
                    publicKeyB64 = preKeyRecord.keyPair.publicKey.serialize().b64(),
                )
            },
        )
    }

    suspend fun persistBootstrapResult(response: SignalBootstrapResponseDto) {
        signalStore.updateBootstrapInfo(
            activeSignedPreKeyId = response.activeSignedPreKeyId,
            activeKyberPreKeyId = response.activeKyberPreKeyId,
            oneTimePreKeysStored = response.oneTimePreKeysStored,
        )
        signalStore.markBootstrapCompleted()
        // Update the last one-time prekey ID to track which IDs we've already sent
        if (response.oneTimePreKeysStored > 0) {
            val lastId = signalStore.getLastOneTimePreKeyId() + response.oneTimePreKeysStored
            signalStore.updateLastOneTimePreKeyId(lastId)
        }
    }

    private fun ByteArray.b64(): String = android.util.Base64.encodeToString(this, android.util.Base64.NO_WRAP)
}


