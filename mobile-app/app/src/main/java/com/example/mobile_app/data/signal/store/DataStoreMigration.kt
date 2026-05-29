package com.example.mobile_app.data.signal.store

import android.content.Context
import com.example.mobile_app.data.signal.store.db.BootstrapMetaEntity
import com.example.mobile_app.data.signal.store.db.SignalDatabase
import com.example.mobile_app.domain.signal.SignalStore

/**
 * Migra una sola vez el material del viejo SignalStore (DataStore) al PersistentSignalProtocolStore.
 * Idempotente: si ya hay identidad propia o meta.migrated=true, no hace nada.
 *
 * Deja intacto el DataStore antiguo (solo marca migrated=true); un task de limpieza posterior puede
 * vaciarlo una vez verificado en producción.
 */
class DataStoreMigration(
    private val context: Context,
    private val store: PersistentSignalProtocolStore,
    private val db: SignalDatabase,
    private val legacy: SignalStore,
) {
    suspend fun migrateIfNeeded() {
        if (store.identity.hasOwnIdentity()) {
            markMigrated(); return
        }
        val material = legacy.loadBootstrapMaterial() ?: run { markMigrated(); return }

        store.identity.initializeOwnIdentity(material.identityKeyPair, material.registrationId)
        store.storeSignedPreKey(material.signedPreKeyRecord.id, material.signedPreKeyRecord)
        store.storeKyberPreKey(material.kyberPreKeyRecord.id, material.kyberPreKeyRecord)
        material.oneTimePreKeyRecords.forEach { store.storePreKey(it.id, it) }
        markMigrated(material.oneTimePreKeyRecords.lastOrNull()?.id ?: 0)
    }

    private fun markMigrated(lastOneTimeId: Int = 0) {
        val existing = db.bootstrapMetaDao().get()
        db.bootstrapMetaDao().upsert(
            BootstrapMetaEntity(
                id = 0, migrated = true,
                lastOneTimePreKeyId = existing?.lastOneTimePreKeyId ?: lastOneTimeId,
            )
        )
    }
}
