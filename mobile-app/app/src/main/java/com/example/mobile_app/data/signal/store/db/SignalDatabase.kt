package com.example.mobile_app.data.signal.store.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        OwnIdentityEntity::class, SignedPreKeyEntity::class, KyberPreKeyEntity::class,
        OneTimePreKeyEntity::class, SessionEntity::class, SenderKeyEntity::class,
        SenderKeyDistributionEntity::class, RemoteIdentityEntity::class, BootstrapMetaEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class SignalDatabase : RoomDatabase() {
    abstract fun ownIdentityDao(): OwnIdentityDao
    abstract fun signedPreKeyDao(): SignedPreKeyDao
    abstract fun kyberPreKeyDao(): KyberPreKeyDao
    abstract fun oneTimePreKeyDao(): OneTimePreKeyDao
    abstract fun sessionDao(): SessionDao
    abstract fun senderKeyDao(): SenderKeyDao
    abstract fun senderKeyDistributionDao(): SenderKeyDistributionDao
    abstract fun remoteIdentityDao(): RemoteIdentityDao
    abstract fun bootstrapMetaDao(): BootstrapMetaDao

    companion object {
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS sender_key_distribution (" +
                        "`key` TEXT NOT NULL PRIMARY KEY, distributionId TEXT NOT NULL, " +
                        "memberUserId INTEGER NOT NULL, sent INTEGER NOT NULL)"
                )
            }
        }

        @Volatile private var INSTANCE: SignalDatabase? = null
        fun get(context: Context): SignalDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext, SignalDatabase::class.java, "signal_store.db",
            ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
