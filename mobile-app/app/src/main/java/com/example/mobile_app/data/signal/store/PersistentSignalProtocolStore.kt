package com.example.mobile_app.data.signal.store

import com.example.mobile_app.data.signal.store.crypto.BlobCipher
import com.example.mobile_app.data.signal.store.db.SignalDatabase
import com.example.mobile_app.domain.signal.IdentityEventBus
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.state.IdentityKeyStore
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SignalProtocolStore
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import java.util.UUID

/**
 * Implementa SignalProtocolStore delegando en los stores Room (incluido SenderKeyStore, que en
 * libsignal 0.86.5 forma parte de SignalProtocolStore). Todas las llamadas son síncronas (las
 * invoca libsignal); ejecutar desde Dispatchers.IO.
 */
class PersistentSignalProtocolStore(
    val db: SignalDatabase,
    cipher: BlobCipher,
    eventBus: IdentityEventBus,
) : SignalProtocolStore {

    val identity = RoomIdentityKeyStore(db.ownIdentityDao(), db.remoteIdentityDao(), cipher, eventBus)
    private val preKeys = RoomPreKeyStore(db.oneTimePreKeyDao(), cipher)
    private val signed = RoomSignedPreKeyStore(db.signedPreKeyDao(), cipher)
    private val kyber = RoomKyberPreKeyStore(db.kyberPreKeyDao(), cipher)
    private val sessions = RoomSessionStore(db.sessionDao(), cipher)
    val senderKeys = RoomSenderKeyStore(db.senderKeyDao(), cipher)

    fun senderKeyDao() = db.senderKeyDao()

    // IdentityKeyStore
    override fun getIdentityKeyPair(): IdentityKeyPair = identity.identityKeyPair
    override fun getLocalRegistrationId(): Int = identity.localRegistrationId
    override fun saveIdentity(a: SignalProtocolAddress, k: IdentityKey): IdentityKeyStore.IdentityChange =
        identity.saveIdentity(a, k)
    override fun isTrustedIdentity(a: SignalProtocolAddress, k: IdentityKey, d: IdentityKeyStore.Direction) =
        identity.isTrustedIdentity(a, k, d)
    override fun getIdentity(a: SignalProtocolAddress): IdentityKey? = identity.getIdentity(a)

    // PreKeyStore
    override fun loadPreKey(id: Int): PreKeyRecord = preKeys.loadPreKey(id)
    override fun storePreKey(id: Int, r: PreKeyRecord) = preKeys.storePreKey(id, r)
    override fun containsPreKey(id: Int): Boolean = preKeys.containsPreKey(id)
    override fun removePreKey(id: Int) = preKeys.removePreKey(id)

    // SignedPreKeyStore
    override fun loadSignedPreKey(id: Int): SignedPreKeyRecord = signed.loadSignedPreKey(id)
    override fun loadSignedPreKeys(): MutableList<SignedPreKeyRecord> = signed.loadSignedPreKeys()
    override fun storeSignedPreKey(id: Int, r: SignedPreKeyRecord) = signed.storeSignedPreKey(id, r)
    override fun containsSignedPreKey(id: Int): Boolean = signed.containsSignedPreKey(id)
    override fun removeSignedPreKey(id: Int) = signed.removeSignedPreKey(id)

    // KyberPreKeyStore
    override fun loadKyberPreKey(id: Int): KyberPreKeyRecord = kyber.loadKyberPreKey(id)
    override fun loadKyberPreKeys(): MutableList<KyberPreKeyRecord> = kyber.loadKyberPreKeys()
    override fun storeKyberPreKey(id: Int, r: KyberPreKeyRecord) = kyber.storeKyberPreKey(id, r)
    override fun containsKyberPreKey(id: Int): Boolean = kyber.containsKyberPreKey(id)
    override fun markKyberPreKeyUsed(kyberPreKeyId: Int, signedPreKeyId: Int, baseKey: ECPublicKey) =
        kyber.markKyberPreKeyUsed(kyberPreKeyId, signedPreKeyId, baseKey)

    // SessionStore
    override fun loadSession(a: SignalProtocolAddress): SessionRecord = sessions.loadSession(a)
    override fun loadExistingSessions(addrs: List<SignalProtocolAddress>): MutableList<SessionRecord> =
        sessions.loadExistingSessions(addrs)
    override fun getSubDeviceSessions(name: String): MutableList<Int> = sessions.getSubDeviceSessions(name)
    override fun storeSession(a: SignalProtocolAddress, r: SessionRecord) = sessions.storeSession(a, r)
    override fun containsSession(a: SignalProtocolAddress): Boolean = sessions.containsSession(a)
    override fun deleteSession(a: SignalProtocolAddress) = sessions.deleteSession(a)
    override fun deleteAllSessions(name: String) = sessions.deleteAllSessions(name)

    // SenderKeyStore
    override fun storeSenderKey(sender: SignalProtocolAddress, distributionId: UUID, record: SenderKeyRecord) =
        senderKeys.storeSenderKey(sender, distributionId, record)
    override fun loadSenderKey(sender: SignalProtocolAddress, distributionId: UUID): SenderKeyRecord? =
        senderKeys.loadSenderKey(sender, distributionId)
}
