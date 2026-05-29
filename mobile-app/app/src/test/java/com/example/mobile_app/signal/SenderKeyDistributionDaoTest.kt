package com.example.mobile_app.signal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.mobile_app.data.signal.store.db.SenderKeyDistributionEntity
import com.example.mobile_app.data.signal.store.db.SignalDatabase
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SenderKeyDistributionDaoTest {
    private lateinit var db: SignalDatabase
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SignalDatabase::class.java,
        ).allowMainThreadQueries().build()
    }
    @After fun teardown() = db.close()

    @Test fun trackAndClear() {
        val dao = db.senderKeyDistributionDao()
        dao.upsert(SenderKeyDistributionEntity("abc|7", "abc", 7, true))
        assertTrue(dao.get("abc", 7)!!.sent)
        dao.clearForDistribution("abc")
        assertNull(dao.get("abc", 7))
    }
}
