package com.example.mobile_app.domain.signal

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.mobile_app.data.model.signal.SignalBootstrapRequestDto
import com.example.mobile_app.data.model.signal.SignalBootstrapResponseDto
import com.example.mobile_app.data.model.signal.SignalOneTimePreKeyDto
import com.example.mobile_app.data.repository.SignalRepository
import com.example.mobile_app.data.signal.store.PersistentSignalProtocolStore
import com.example.mobile_app.data.signal.store.SignalStoreBootstrap
import com.example.mobile_app.data.signal.store.crypto.KeystoreBlobCipher
import com.example.mobile_app.data.signal.store.db.SignalDatabase
import com.example.mobile_app.security.CurrentUserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Punto único del subsistema Signal en producción. Mantiene el store persistente (Room + Keystore),
 * el [SignalCipherService] y el arranque (generación + publicación de claves) sobre ESE mismo store.
 */
class SignalEngine private constructor(
    context: Context,
    currentUserManager: CurrentUserManager,
    private val signalRepository: SignalRepository,
) {
    private val db = SignalDatabase.get(context)
    val identityEvents = IdentityEventBus()
    private val store = PersistentSignalProtocolStore(db, KeystoreBlobCipher(), identityEvents)
    private val bootstrap = SignalStoreBootstrap(store, db)

    val cipher = SignalCipherService(
        store = store,
        ownUserId = { currentUserManager.getCurrentUser()?.id ?: 0L },
        bundleFetcher = { signalRepository.getUserBundle(it) },
    )

    private val bootstrapMutex = Mutex()

    /**
     * Genera el material local si no existe y publica las claves públicas al backend.
     * Idempotente: las claves no cambian tras la primera generación; el backend acepta el bootstrap.
     */
    suspend fun ensureBootstrapped(): SignalBootstrapResponseDto? = withContext(Dispatchers.IO) {
        bootstrapMutex.withLock {
            bootstrap.initializeIfNeeded()
            val request = buildBootstrapRequest()
            runCatching { signalRepository.bootstrapKeys(request) }
                .onFailure { Log.w(TAG, "bootstrap publish falló: ${it.message}") }
                .getOrNull()
        }
    }

    private fun buildBootstrapRequest(): SignalBootstrapRequestDto {
        fun ByteArray.b64() = Base64.encodeToString(this, Base64.NO_WRAP)
        val signed = store.loadSignedPreKey(SIGNED_PRE_KEY_ID)
        val kyber = store.loadKyberPreKey(KYBER_PRE_KEY_ID)
        val oneTime = db.oneTimePreKeyDao().allUnconsumed().map { store.loadPreKey(it.preKeyId) }
        return SignalBootstrapRequestDto(
            registrationId = store.localRegistrationId,
            identityKeyPublicB64 = store.identityKeyPair.publicKey.serialize().b64(),
            signedPreKeyId = signed.id,
            signedPreKeyPublicB64 = signed.keyPair.publicKey.serialize().b64(),
            signedPreKeySignatureB64 = signed.signature.b64(),
            kyberPreKeyId = kyber.id,
            kyberPreKeyPublicB64 = kyber.keyPair.publicKey.serialize().b64(),
            kyberPreKeySignatureB64 = kyber.signature.b64(),
            oneTimePreKeys = oneTime.map {
                SignalOneTimePreKeyDto(it.id, it.keyPair.publicKey.serialize().b64())
            },
        )
    }

    companion object {
        private const val TAG = "SignalEngine"
        private const val SIGNED_PRE_KEY_ID = 1
        private const val KYBER_PRE_KEY_ID = 1

        @Volatile private var INSTANCE: SignalEngine? = null

        fun get(
            context: Context,
            currentUserManager: CurrentUserManager,
            signalRepository: SignalRepository,
        ): SignalEngine = INSTANCE ?: synchronized(this) {
            INSTANCE ?: SignalEngine(context.applicationContext, currentUserManager, signalRepository)
                .also { INSTANCE = it }
        }
    }
}
