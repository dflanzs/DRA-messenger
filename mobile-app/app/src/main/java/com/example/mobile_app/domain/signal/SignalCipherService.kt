package com.example.mobile_app.domain.signal

import android.util.Base64
import com.example.mobile_app.data.model.signal.SignalBundleResponseDto
import com.example.mobile_app.data.signal.store.PersistentSignalProtocolStore
import com.example.mobile_app.data.signal.store.key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.SessionCipher
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.UntrustedIdentityException
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.kem.KEMPublicKey
import org.signal.libsignal.protocol.message.CiphertextMessage
import org.signal.libsignal.protocol.message.PreKeySignalMessage
import org.signal.libsignal.protocol.message.SignalMessage
import org.signal.libsignal.protocol.state.IdentityKeyStore
import org.signal.libsignal.protocol.state.PreKeyBundle

/** Tipo y bytes del ciphertext, listos para el wire (cypherTextType / cypherTextB64). */
data class EncryptedPayload(val type: Short, val bytes: ByteArray) {
    fun b64(): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
}

class IdentityChangedException(val address: String) : Exception("Identidad cambiada: $address")

/** Resuelve el prekey bundle de un usuario (normalmente vía SignalRepository.getUserBundle). */
fun interface BundleFetcher { suspend fun fetch(remoteUserId: Long): SignalBundleResponseDto }

class SignalCipherService(
    private val store: PersistentSignalProtocolStore,
    private val ownUserId: Long,
    private val bundleFetcher: BundleFetcher,
) {
    private val mutexes = HashMap<String, Mutex>()
    @Synchronized private fun mutexFor(key: String) = mutexes.getOrPut(key) { Mutex() }

    private fun address(userId: Long) = SignalProtocolAddress(userId.toString(), 1)
    private fun b64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)

    suspend fun encryptDirect(remoteUserId: Long, plaintext: ByteArray): EncryptedPayload =
        withContext(Dispatchers.IO) {
            val addr = address(remoteUserId)
            mutexFor(addr.key()).withLock {
                if (!store.containsSession(addr)) establishSession(remoteUserId, addr)
                val cipher = SessionCipher(store, addr)
                val msg: CiphertextMessage = cipher.encrypt(plaintext)
                EncryptedPayload(msg.type.toShort(), msg.serialize())
            }
        }

    suspend fun decryptDirect(remoteUserId: Long, payload: EncryptedPayload): ByteArray =
        withContext(Dispatchers.IO) {
            val addr = address(remoteUserId)
            mutexFor(addr.key()).withLock {
                val cipher = SessionCipher(store, addr)
                when (payload.type.toInt()) {
                    CiphertextMessage.PREKEY_TYPE ->
                        cipher.decrypt(PreKeySignalMessage(payload.bytes))
                    else ->
                        cipher.decrypt(SignalMessage(payload.bytes))
                }
            }
        }

    private suspend fun establishSession(remoteUserId: Long, addr: SignalProtocolAddress) {
        val b = bundleFetcher.fetch(remoteUserId)
        val identityKey = IdentityKey(b64(b.identityKeyPublicB64))
        // TOFU: si la identidad difiere de la confiada, abortar.
        if (!store.isTrustedIdentity(addr, identityKey, IdentityKeyStore.Direction.SENDING)) {
            throw IdentityChangedException(addr.key())
        }
        val hasOtp = b.oneTimePreKeyId != null && b.oneTimePreKeyPublicB64 != null
        val bundle = PreKeyBundle(
            b.registrationId,
            b.deviceId,
            if (hasOtp) b.oneTimePreKeyId!! else PreKeyBundle.NULL_PRE_KEY_ID,
            if (hasOtp) ECPublicKey(b64(b.oneTimePreKeyPublicB64!!)) else null,
            b.signedPreKeyId,
            ECPublicKey(b64(b.signedPreKeyPublicB64)),
            b64(b.signedPreKeySignatureB64),
            identityKey,
            b.kyberPreKeyId,
            KEMPublicKey(b64(b.kyberPreKeyPublicB64)),
            b64(b.kyberPreKeySignatureB64),
        )
        try {
            SessionBuilder(store, addr).process(bundle)
        } catch (e: UntrustedIdentityException) {
            throw IdentityChangedException(addr.key())
        }
        store.saveIdentity(addr, identityKey)
    }
}

fun EncryptedPayload.toWireB64(): Pair<Short, String> = type to b64()

fun payloadFromWire(cypherTextType: Short, cypherTextB64: String): EncryptedPayload =
    EncryptedPayload(cypherTextType, Base64.decode(cypherTextB64, Base64.NO_WRAP))
