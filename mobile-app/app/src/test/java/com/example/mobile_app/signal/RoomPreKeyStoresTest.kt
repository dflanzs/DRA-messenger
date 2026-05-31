package com.example.mobile_app.signal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.mobile_app.data.signal.store.RoomKyberPreKeyStore
import com.example.mobile_app.data.signal.store.RoomPreKeyStore
import com.example.mobile_app.data.signal.store.RoomSenderKeyStore
import com.example.mobile_app.data.signal.store.RoomSignedPreKeyStore
import com.example.mobile_app.data.signal.store.db.SignalDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.groups.GroupSessionBuilder
import org.signal.libsignal.protocol.kem.KEMKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyType
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class RoomPreKeyStoresTest {
    private lateinit var db: SignalDatabase
    private val cipher = FakeBlobCipher()

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SignalDatabase::class.java,
        ).allowMainThreadQueries().build()
    }
    @After fun teardown() = db.close()

    @Test fun oneTimePreKeyStoreAndRemove() {
        val store = RoomPreKeyStore(db.oneTimePreKeyDao(), cipher)
        val rec = PreKeyRecord(7, ECKeyPair.generate())
        store.storePreKey(7, rec)
        assertTrue(store.containsPreKey(7))
        assertEquals(7, store.loadPreKey(7).id)
        store.removePreKey(7)
        assertFalse(store.containsPreKey(7))
    }

    @Test fun signedPreKeyStore() {
        val store = RoomSignedPreKeyStore(db.signedPreKeyDao(), cipher)
        val idp = IdentityKeyPair.generate()
        val kp = ECKeyPair.generate()
        val rec = SignedPreKeyRecord(1, System.currentTimeMillis(), kp,
            idp.privateKey.calculateSignature(kp.publicKey.serialize()))
        store.storeSignedPreKey(1, rec)
        assertTrue(store.containsSignedPreKey(1))
        assertEquals(1, store.loadSignedPreKey(1).id)
        assertEquals(1, store.loadSignedPreKeys().size)
    }

    @Test fun kyberPreKeyStore() {
        val store = RoomKyberPreKeyStore(db.kyberPreKeyDao(), cipher)
        val idp = IdentityKeyPair.generate()
        val kem = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
        val rec = KyberPreKeyRecord(1, System.currentTimeMillis(), kem,
            idp.privateKey.calculateSignature(kem.publicKey.serialize()))
        store.storeKyberPreKey(1, rec)
        assertTrue(store.containsKyberPreKey(1))
        assertEquals(1, store.loadKyberPreKey(1).id)
    }

    @Test fun senderKeyStoreRoundTripAndAbsentNull() {
        val store = RoomSenderKeyStore(db.senderKeyDao(), cipher)
        val sender = SignalProtocolAddress("3", 1)
        val dist = UUID.fromString("00000000-0000-0000-0000-000000000007")
        // Ausente -> null
        assertNull(store.loadSenderKey(sender, dist))
        // GroupSessionBuilder.create persiste un SenderKeyRecord real a través del store.
        GroupSessionBuilder(store).create(sender, dist)
        assertNotNull(store.loadSenderKey(sender, dist))
    }
}
