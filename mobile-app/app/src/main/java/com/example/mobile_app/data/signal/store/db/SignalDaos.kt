package com.example.mobile_app.data.signal.store.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OwnIdentityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(e: OwnIdentityEntity)
    @Query("SELECT * FROM own_identity WHERE id = 0") fun get(): OwnIdentityEntity?
}

@Dao
interface SignedPreKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(e: SignedPreKeyEntity)
    @Query("SELECT * FROM signed_prekeys WHERE preKeyId = :id") fun get(id: Int): SignedPreKeyEntity?
    @Query("SELECT * FROM signed_prekeys") fun all(): List<SignedPreKeyEntity>
    @Query("DELETE FROM signed_prekeys WHERE preKeyId = :id") fun delete(id: Int)
    @Query("SELECT COUNT(*) FROM signed_prekeys WHERE preKeyId = :id") fun count(id: Int): Int
}

@Dao
interface KyberPreKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(e: KyberPreKeyEntity)
    @Query("SELECT * FROM kyber_prekeys WHERE preKeyId = :id") fun get(id: Int): KyberPreKeyEntity?
    @Query("SELECT * FROM kyber_prekeys") fun all(): List<KyberPreKeyEntity>
    @Query("SELECT COUNT(*) FROM kyber_prekeys WHERE preKeyId = :id") fun count(id: Int): Int
}

@Dao
interface OneTimePreKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(e: OneTimePreKeyEntity)
    @Query("SELECT * FROM one_time_prekeys WHERE preKeyId = :id") fun get(id: Int): OneTimePreKeyEntity?
    @Query("DELETE FROM one_time_prekeys WHERE preKeyId = :id") fun delete(id: Int)
    @Query("SELECT COUNT(*) FROM one_time_prekeys WHERE preKeyId = :id AND consumed = 0") fun countUnconsumed(id: Int): Int
    @Query("SELECT COUNT(*) FROM one_time_prekeys WHERE consumed = 0") fun remaining(): Int
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(e: SessionEntity)
    @Query("SELECT * FROM sessions WHERE address = :address") fun get(address: String): SessionEntity?
    @Query("SELECT address FROM sessions WHERE address LIKE :prefix") fun addressesWithPrefix(prefix: String): List<String>
    @Query("SELECT COUNT(*) FROM sessions WHERE address = :address") fun count(address: String): Int
    @Query("DELETE FROM sessions WHERE address = :address") fun delete(address: String)
    @Query("DELETE FROM sessions WHERE address LIKE :prefix") fun deleteWithPrefix(prefix: String)
}

@Dao
interface SenderKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(e: SenderKeyEntity)
    @Query("SELECT * FROM sender_keys WHERE `key` = :k") fun get(k: String): SenderKeyEntity?
    @Query("DELETE FROM sender_keys WHERE `key` = :k") fun delete(k: String)
}

@Dao
interface RemoteIdentityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(e: RemoteIdentityEntity)
    @Query("SELECT * FROM remote_identities WHERE address = :address") fun get(address: String): RemoteIdentityEntity?
}

@Dao
interface BootstrapMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(e: BootstrapMetaEntity)
    @Query("SELECT * FROM bootstrap_meta WHERE id = 0") fun get(): BootstrapMetaEntity?
}
