package com.example.mobile_app.signal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.mobile_app.data.signal.store.RoomSessionStore
import com.example.mobile_app.data.signal.store.db.SignalDatabase
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.state.SessionRecord

@RunWith(RobolectricTestRunner::class)
class RoomSessionStoreTest {
    private lateinit var db: SignalDatabase
    private lateinit var store: RoomSessionStore

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SignalDatabase::class.java,
        ).allowMainThreadQueries().build()
        store = RoomSessionStore(db.sessionDao(), FakeBlobCipher())
    }
    @After fun teardown() = db.close()

    @Test fun storeContainsDelete() {
        val addr = SignalProtocolAddress("bob", 1)
        assertFalse(store.containsSession(addr))
        store.storeSession(addr, SessionRecord())
        assertTrue(store.containsSession(addr))
        store.deleteSession(addr)
        assertFalse(store.containsSession(addr))
    }
}
