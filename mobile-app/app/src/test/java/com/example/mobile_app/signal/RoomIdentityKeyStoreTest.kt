package com.example.mobile_app.signal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.mobile_app.data.signal.store.RoomIdentityKeyStore
import com.example.mobile_app.data.signal.store.db.SignalDatabase
import com.example.mobile_app.domain.signal.IdentityEventBus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.state.IdentityKeyStore

@RunWith(RobolectricTestRunner::class)
class RoomIdentityKeyStoreTest {
    private lateinit var db: SignalDatabase
    private lateinit var store: RoomIdentityKeyStore
    private lateinit var bus: IdentityEventBus

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SignalDatabase::class.java,
        ).allowMainThreadQueries().build()
        bus = IdentityEventBus()
        store = RoomIdentityKeyStore(db.ownIdentityDao(), db.remoteIdentityDao(), FakeBlobCipher(), bus)
        store.initializeOwnIdentity(IdentityKeyPair.generate(), registrationId = 42)
    }

    @After fun teardown() = db.close()

    @Test fun tofuAcceptsFirstThenBlocksChange() {
        val addr = SignalProtocolAddress("bob", 1)
        val firstKey: IdentityKey = IdentityKeyPair.generate().publicKey
        // Primera vez: confiada y guardada.
        assertTrue(store.isTrustedIdentity(addr, firstKey, IdentityKeyStore.Direction.SENDING))
        assertEquals(IdentityKeyStore.IdentityChange.NEW_OR_UNCHANGED, store.saveIdentity(addr, firstKey))
        assertTrue(store.isTrustedIdentity(addr, firstKey, IdentityKeyStore.Direction.SENDING))

        // Cambio de identidad -> no confiada.
        val changedKey: IdentityKey = IdentityKeyPair.generate().publicKey
        assertFalse(store.isTrustedIdentity(addr, changedKey, IdentityKeyStore.Direction.SENDING))
        // Si se acepta el cambio, saveIdentity informa REPLACED_EXISTING.
        assertEquals(IdentityKeyStore.IdentityChange.REPLACED_EXISTING, store.saveIdentity(addr, changedKey))
    }

    @Test fun localRegistrationIdPersisted() {
        assertEquals(42, store.getLocalRegistrationId())
    }
}
