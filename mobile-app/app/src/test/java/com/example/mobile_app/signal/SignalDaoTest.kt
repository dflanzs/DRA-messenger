package com.example.mobile_app.signal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.mobile_app.data.signal.store.db.SessionEntity
import com.example.mobile_app.data.signal.store.db.SignalDatabase
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SignalDaoTest {
    private lateinit var db: SignalDatabase

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SignalDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After fun teardown() = db.close()

    @Test fun sessionUpsertGetDelete() {
        val dao = db.sessionDao()
        dao.upsert(SessionEntity("alice:1", byteArrayOf(9, 8, 7)))
        assertArrayEquals(byteArrayOf(9, 8, 7), dao.get("alice:1")!!.recordEnc)
        assertEquals(1, dao.count("alice:1"))
        dao.delete("alice:1")
        assertNull(dao.get("alice:1"))
    }

    @Test fun addressPrefixQuery() {
        val dao = db.sessionDao()
        dao.upsert(SessionEntity("bob:1", byteArrayOf(1)))
        dao.upsert(SessionEntity("bob:2", byteArrayOf(2)))
        dao.upsert(SessionEntity("carol:1", byteArrayOf(3)))
        assertEquals(2, dao.addressesWithPrefix("bob:%").size)
    }
}
