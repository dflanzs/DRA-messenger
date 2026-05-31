package com.example.mobile_app.data.signal.store

import com.example.mobile_app.data.signal.store.crypto.BlobCipher
import com.example.mobile_app.data.signal.store.db.SessionDao
import com.example.mobile_app.data.signal.store.db.SessionEntity
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SessionStore

class RoomSessionStore(
    private val dao: SessionDao,
    private val cipher: BlobCipher,
) : SessionStore {
    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        val e = dao.get(address.key()) ?: return SessionRecord()
        return SessionRecord(cipher.decrypt(e.recordEnc))
    }
    override fun loadExistingSessions(addresses: List<SignalProtocolAddress>): MutableList<SessionRecord> =
        addresses.map { addr ->
            val e = dao.get(addr.key()) ?: error("No session for ${addr.key()}")
            SessionRecord(cipher.decrypt(e.recordEnc))
        }.toMutableList()
    override fun getSubDeviceSessions(name: String): MutableList<Int> =
        dao.addressesWithPrefix("$name:%")
            .mapNotNull { it.substringAfter(":").toIntOrNull() }
            .filter { it != 1 }
            .toMutableList()
    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        dao.upsert(SessionEntity(address.key(), cipher.encrypt(record.serialize())))
    }
    override fun containsSession(address: SignalProtocolAddress): Boolean = dao.count(address.key()) > 0
    override fun deleteSession(address: SignalProtocolAddress) = dao.delete(address.key())
    override fun deleteAllSessions(name: String) = dao.deleteWithPrefix("$name:%")
}
