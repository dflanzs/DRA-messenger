package com.example.mobile_app.signal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.mobile_app.data.model.signal.SignalBundleResponseDto
import com.example.mobile_app.data.signal.store.PersistentSignalProtocolStore
import com.example.mobile_app.data.signal.store.SignalStoreBootstrap
import com.example.mobile_app.data.signal.store.db.SignalDatabase
import com.example.mobile_app.domain.signal.IdentityChangedException
import com.example.mobile_app.domain.signal.IdentityEventBus
import com.example.mobile_app.domain.signal.SignalCipherService
import android.util.Base64
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.signal.libsignal.protocol.SignalProtocolAddress

@RunWith(RobolectricTestRunner::class)
class CipherServiceTofuTest {
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

    @Test fun identityChangeAborts() {
        val aliceStore = peer()
        val bob1 = peer()
        val bob2 = peer() // "otro Bob" con identidad distinta

        var current = bob1
        val alice = SignalCipherService(aliceStore, ownUserId = 1) { bundleOf(current) }

        runBlocking { alice.encryptDirect(2, "hola".toByteArray()) } // primera sesión OK con bob1

        // Forzar reestablecimiento con identidad distinta: borrar sesión y cambiar bundle a bob2.
        aliceStore.deleteSession(SignalProtocolAddress("2", 1))
        current = bob2
        assertThrows(IdentityChangedException::class.java) {
            runBlocking { alice.encryptDirect(2, "otra vez".toByteArray()) }
        }
    }
}
