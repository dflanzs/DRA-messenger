package com.example.mobile_app.data.signal.store

import com.example.mobile_app.data.signal.store.crypto.BlobCipher
import com.example.mobile_app.data.signal.store.db.OwnIdentityDao
import com.example.mobile_app.data.signal.store.db.OwnIdentityEntity
import com.example.mobile_app.data.signal.store.db.RemoteIdentityDao
import com.example.mobile_app.data.signal.store.db.RemoteIdentityEntity
import com.example.mobile_app.domain.signal.IdentityEventBus
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.state.IdentityKeyStore

fun SignalProtocolAddress.key(): String = "${name}:${deviceId}"

class RoomIdentityKeyStore(
    private val ownDao: OwnIdentityDao,
    private val remoteDao: RemoteIdentityDao,
    private val cipher: BlobCipher,
    private val eventBus: IdentityEventBus,
) : IdentityKeyStore {

    fun initializeOwnIdentity(pair: IdentityKeyPair, registrationId: Int) {
        if (ownDao.get() == null) {
            ownDao.upsert(OwnIdentityEntity(0, registrationId, cipher.encrypt(pair.serialize())))
        }
    }

    fun hasOwnIdentity(): Boolean = ownDao.get() != null

    override fun getIdentityKeyPair(): IdentityKeyPair {
        val e = ownDao.get() ?: error("Own identity not initialized")
        return IdentityKeyPair(cipher.decrypt(e.identityKeyPairEnc))
    }

    override fun getLocalRegistrationId(): Int =
        ownDao.get()?.registrationId ?: error("Own identity not initialized")

    override fun saveIdentity(address: SignalProtocolAddress, identity: IdentityKey): IdentityKeyStore.IdentityChange {
        val existing = remoteDao.get(address.key())
        val changed = existing != null && !existing.identityKey.contentEquals(identity.serialize())
        remoteDao.upsert(
            RemoteIdentityEntity(
                address = address.key(),
                identityKey = identity.serialize(),
                trusted = true,
                firstSeenAt = existing?.firstSeenAt ?: System.currentTimeMillis(),
            )
        )
        return if (changed) IdentityKeyStore.IdentityChange.REPLACED_EXISTING
        else IdentityKeyStore.IdentityChange.NEW_OR_UNCHANGED
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identity: IdentityKey,
        direction: IdentityKeyStore.Direction,
    ): Boolean {
        val existing = remoteDao.get(address.key()) ?: return true // TOFU: primera vez
        val matches = existing.identityKey.contentEquals(identity.serialize())
        if (!matches) eventBus.emitChange(address.key())
        return matches
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? =
        remoteDao.get(address.key())?.let { IdentityKey(it.identityKey) }
}
