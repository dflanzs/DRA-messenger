package com.example.mobile_app.signal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.mobile_app.data.model.signal.SignalBundleResponseDto
import com.example.mobile_app.data.signal.store.PersistentSignalProtocolStore
import com.example.mobile_app.data.signal.store.SignalStoreBootstrap
import com.example.mobile_app.data.signal.store.db.SignalDatabase
import com.example.mobile_app.domain.signal.EncryptedPayload
import com.example.mobile_app.domain.signal.IdentityEventBus
import com.example.mobile_app.domain.signal.SignalCipherService
import android.util.Base64
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlinx.coroutines.runBlocking
import org.signal.libsignal.protocol.DuplicateMessageException

/**
 * Evidencia reproducible de propiedades del DRA para la memoria.
 *
 * Forward secrecy / no-replay: cada mensaje usa una message key de un solo uso derivada de la
 * cadena KDF; al descifrarlo, la cadena avanza y la clave se descarta. Reintentar el MISMO
 * ciphertext ya no produce el plaintext (lanza DuplicateMessageException): un atacante que capture
 * tráfico y más tarde comprometa el estado no puede recuperar mensajes pasados.
 */
@RunWith(RobolectricTestRunner::class)
class DraPropertiesTest {
    private fun peer(): PersistentSignalProtocolStore {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SignalDatabase::class.java,
        ).allowMainThreadQueries().build()
        val store = PersistentSignalProtocolStore(db, FakeBlobCipher(), IdentityEventBus())
        SignalStoreBootstrap(store, db, oneTimeCount = 10).initializeIfNeeded()
        return store
    }

    private fun bundleOf(store: PersistentSignalProtocolStore): SignalBundleResponseDto {
        val s = store.loadSignedPreKey(1); val k = store.loadKyberPreKey(1); val o = store.loadPreKey(1)
        fun ByteArray.b64() = Base64.encodeToString(this, Base64.NO_WRAP)
        return SignalBundleResponseDto(
            store.localRegistrationId, 1, store.identityKeyPair.publicKey.serialize().b64(),
            1, s.keyPair.publicKey.serialize().b64(), s.signature.b64(),
            1, k.keyPair.publicKey.serialize().b64(), k.signature.b64(),
            1, o.keyPair.publicKey.serialize().b64(),
        )
    }

    @Test fun forwardSecrecy_consumedMessageKeyCannotBeReused() = runTest {
        val aliceStore = peer(); val bobStore = peer()
        val alice = SignalCipherService(aliceStore, ownUserId = { 1 }) { bundleOf(bobStore) }
        val bob = SignalCipherService(bobStore, ownUserId = { 2 }) { bundleOf(aliceStore) }

        val captured: EncryptedPayload = alice.encryptDirect(2, "secreto".toByteArray())
        assertEquals("secreto", String(bob.decryptDirect(1, captured)))

        // Reintentar el mismo ciphertext: la message key ya se consumió -> DuplicateMessageException.
        assertThrows(DuplicateMessageException::class.java) {
            runBlocking { bob.decryptDirect(1, captured) }
        }
    }
}
