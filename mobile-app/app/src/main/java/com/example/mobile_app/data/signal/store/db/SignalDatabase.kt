package com.example.mobile_app.data.signal.store.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        OwnIdentityEntity::class, SignedPreKeyEntity::class, KyberPreKeyEntity::class,
        OneTimePreKeyEntity::class, SessionEntity::class, SenderKeyEntity::class,
        RemoteIdentityEntity::class, BootstrapMetaEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SignalDatabase : RoomDatabase() {
    abstract fun ownIdentityDao(): OwnIdentityDao
    abstract fun signedPreKeyDao(): SignedPreKeyDao
    abstract fun kyberPreKeyDao(): KyberPreKeyDao
    abstract fun oneTimePreKeyDao(): OneTimePreKeyDao
    abstract fun sessionDao(): SessionDao
    abstract fun senderKeyDao(): SenderKeyDao
    abstract fun remoteIdentityDao(): RemoteIdentityDao
    abstract fun bootstrapMetaDao(): BootstrapMetaDao

    companion object {
        @Volatile private var INSTANCE: SignalDatabase? = null
        fun get(context: Context): SignalDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext, SignalDatabase::class.java, "signal_store.db",
            ).build().also { INSTANCE = it }
        }
    }
}
