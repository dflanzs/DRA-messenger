package com.example.mobile_app.signal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.mobile_app.data.signal.store.PersistentSignalProtocolStore
import com.example.mobile_app.data.signal.store.SignalStoreBootstrap
import com.example.mobile_app.data.signal.store.db.SignalDatabase
import com.example.mobile_app.domain.signal.IdentityEventBus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PersistentStoreBootstrapTest {
    private lateinit var db: SignalDatabase
    private lateinit var store: PersistentSignalProtocolStore

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SignalDatabase::class.java,
        ).allowMainThreadQueries().build()
        store = PersistentSignalProtocolStore(db, FakeBlobCipher(), IdentityEventBus())
    }
    @After fun teardown() = db.close()

    @Test fun exposesSenderKeyStore() {
        assertNotNull(store.senderKeys)
    }

    @Test fun bootstrapIsIdempotent() {
        val b = SignalStoreBootstrap(store, db, oneTimeCount = 5)
        b.initializeIfNeeded()
        val regId = store.localRegistrationId
        val firstSigned = store.loadSignedPreKeys().single().id
        b.initializeIfNeeded() // segunda vez no debe regenerar
        assertEquals(regId, store.localRegistrationId)
        assertEquals(firstSigned, store.loadSignedPreKeys().single().id)
        assertTrue(store.identityKeyPair.serialize().isNotEmpty())
    }
}
