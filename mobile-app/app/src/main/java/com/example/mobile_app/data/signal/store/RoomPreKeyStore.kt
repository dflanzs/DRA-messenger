package com.example.mobile_app.data.signal.store

import com.example.mobile_app.data.signal.store.crypto.BlobCipher
import com.example.mobile_app.data.signal.store.db.OneTimePreKeyDao
import com.example.mobile_app.data.signal.store.db.OneTimePreKeyEntity
import org.signal.libsignal.protocol.InvalidKeyIdException
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyStore

class RoomPreKeyStore(
    private val dao: OneTimePreKeyDao,
    private val cipher: BlobCipher,
) : PreKeyStore {
    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        val e = dao.get(preKeyId) ?: throw InvalidKeyIdException("No one-time prekey $preKeyId")
        return PreKeyRecord(cipher.decrypt(e.recordEnc))
    }
    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        dao.upsert(OneTimePreKeyEntity(preKeyId, cipher.encrypt(record.serialize()), consumed = false))
    }
    override fun containsPreKey(preKeyId: Int): Boolean = dao.countUnconsumed(preKeyId) > 0
    override fun removePreKey(preKeyId: Int) = dao.delete(preKeyId)
}
