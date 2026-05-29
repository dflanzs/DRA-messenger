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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CipherServiceRoundTripTest {

    private fun newPeer(): PersistentSignalProtocolStore {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SignalDatabase::class.java,
        ).allowMainThreadQueries().build()
        val store = PersistentSignalProtocolStore(db, FakeBlobCipher(), IdentityEventBus())
        SignalStoreBootstrap(store, db, oneTimeCount = 10).initializeIfNeeded()
        return store
    }

    private fun bundleOf(store: PersistentSignalProtocolStore): SignalBundleResponseDto {
        val signed = store.loadSignedPreKey(1)
        val kyber = store.loadKyberPreKey(1)
        val otp = store.loadPreKey(1)
        fun ByteArray.b64() = Base64.encodeToString(this, Base64.NO_WRAP)
        return SignalBundleResponseDto(
            registrationId = store.localRegistrationId,
            deviceId = 1,
            identityKeyPublicB64 = store.identityKeyPair.publicKey.serialize().b64(),
            signedPreKeyId = 1,
            signedPreKeyPublicB64 = signed.keyPair.publicKey.serialize().b64(),
            signedPreKeySignatureB64 = signed.signature.b64(),
            kyberPreKeyId = 1,
            kyberPreKeyPublicB64 = kyber.keyPair.publicKey.serialize().b64(),
            kyberPreKeySignatureB64 = kyber.signature.b64(),
            oneTimePreKeyId = 1,
            oneTimePreKeyPublicB64 = otp.keyPair.publicKey.serialize().b64(),
        )
    }

    @Test fun aliceToBobRoundTripAndOutOfOrder() = runTest {
        val aliceStore = newPeer()
        val bobStore = newPeer()

        val alice = SignalCipherService(aliceStore, ownUserId = { 1 }) { bundleOf(bobStore) }
        val bob = SignalCipherService(bobStore, ownUserId = { 2 }) { bundleOf(aliceStore) }

        // Alice abre sesión: primer mensaje es PreKeySignalMessage (type=3).
        val m1 = alice.encryptDirect(remoteUserId = 2, plaintext = "uno".toByteArray())
        assertEquals(3, m1.type.toInt())
        assertEquals("uno", String(bob.decryptDirect(remoteUserId = 1, m1)))

        // Bob responde -> al descifrarlo, la sesión de Alice pasa a SignalMessage (type=2).
        val r1 = bob.encryptDirect(remoteUserId = 1, plaintext = "re".toByteArray())
        assertEquals("re", String(alice.decryptDirect(remoteUserId = 2, r1)))

        // Ahora los mensajes de Alice son whisper (type=2).
        val m2 = alice.encryptDirect(2, "dos".toByteArray())
        val m3 = alice.encryptDirect(2, "tres".toByteArray())
        assertEquals(2, m3.type.toInt())

        // Bob los descifra fuera de orden: 3, luego 2.
        suspend fun dec(p: EncryptedPayload) = String(bob.decryptDirect(remoteUserId = 1, p))
        assertEquals("tres", dec(m3))
        assertEquals("dos", dec(m2))
    }
}
