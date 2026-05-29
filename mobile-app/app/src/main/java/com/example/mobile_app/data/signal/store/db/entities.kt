package com.example.mobile_app.data.signal.store.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "own_identity")
data class OwnIdentityEntity(
    @PrimaryKey val id: Int = 0,           // singleton (siempre 0)
    val registrationId: Int,
    val identityKeyPairEnc: ByteArray,     // cifrado: IdentityKeyPair.serialize()
)

@Entity(tableName = "signed_prekeys")
data class SignedPreKeyEntity(
    @PrimaryKey val preKeyId: Int,
    val recordEnc: ByteArray,              // cifrado: SignedPreKeyRecord.serialize()
    val active: Boolean,
)

@Entity(tableName = "kyber_prekeys")
data class KyberPreKeyEntity(
    @PrimaryKey val preKeyId: Int,
    val recordEnc: ByteArray,              // cifrado: KyberPreKeyRecord.serialize()
    val active: Boolean,
)

@Entity(tableName = "one_time_prekeys")
data class OneTimePreKeyEntity(
    @PrimaryKey val preKeyId: Int,
    val recordEnc: ByteArray,              // cifrado: PreKeyRecord.serialize()
    val consumed: Boolean = false,
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val address: String,       // "name:deviceId"
    val recordEnc: ByteArray,              // cifrado: SessionRecord.serialize()
)

@Entity(tableName = "sender_keys")
data class SenderKeyEntity(
    @PrimaryKey val key: String,           // "senderName:senderDeviceId|distributionId"
    val recordEnc: ByteArray,              // cifrado: SenderKeyRecord.serialize()
)

/** Rastrea a qué miembros se les ha enviado ya nuestra SKDM para una distributionId concreta. */
@Entity(tableName = "sender_key_distribution")
data class SenderKeyDistributionEntity(
    @PrimaryKey val key: String,           // "distributionId|memberUserId"
    val distributionId: String,
    val memberUserId: Long,
    val sent: Boolean,
)

@Entity(tableName = "remote_identities")
data class RemoteIdentityEntity(
    @PrimaryKey val address: String,       // "name:deviceId"
    val identityKey: ByteArray,            // pública (sin cifrar): IdentityKey.serialize()
    val trusted: Boolean,
    val firstSeenAt: Long,
)

@Entity(tableName = "bootstrap_meta")
data class BootstrapMetaEntity(
    @PrimaryKey val id: Int = 0,
    val migrated: Boolean = false,
    val lastOneTimePreKeyId: Int = 0,
)
