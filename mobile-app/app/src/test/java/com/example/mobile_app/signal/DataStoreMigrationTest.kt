package com.example.mobile_app.signal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.mobile_app.data.signal.store.DataStoreMigration
import com.example.mobile_app.data.signal.store.PersistentSignalProtocolStore
import com.example.mobile_app.data.signal.store.db.SignalDatabase
import com.example.mobile_app.domain.signal.IdentityEventBus
import com.example.mobile_app.domain.signal.SignalStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataStoreMigrationTest {
    @Test fun migratesLegacyMaterial() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Sembrar el viejo DataStore.
        val legacy = SignalStore(ctx)
        val material = legacy.loadOrCreateBootstrapMaterial()

        val db = Room.inMemoryDatabaseBuilder(ctx, SignalDatabase::class.java)
            .allowMainThreadQueries().build()
        val store = PersistentSignalProtocolStore(db, FakeBlobCipher(), IdentityEventBus())

        DataStoreMigration(ctx, store, db, legacy).migrateIfNeeded()

        assertEquals(material.registrationId, store.localRegistrationId)
        assertTrue(store.containsSignedPreKey(material.signedPreKeyRecord.id))
        assertTrue(store.containsKyberPreKey(material.kyberPreKeyRecord.id))
        assertEquals(true, db.bootstrapMetaDao().get()!!.migrated)
    }
}
