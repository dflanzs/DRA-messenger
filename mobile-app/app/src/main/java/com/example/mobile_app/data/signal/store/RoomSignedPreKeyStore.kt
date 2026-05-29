package com.example.mobile_app.data.signal.store

import com.example.mobile_app.data.signal.store.crypto.BlobCipher
import com.example.mobile_app.data.signal.store.db.SignedPreKeyDao
import com.example.mobile_app.data.signal.store.db.SignedPreKeyEntity
import org.signal.libsignal.protocol.InvalidKeyIdException
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyStore

class RoomSignedPreKeyStore(
    private val dao: SignedPreKeyDao,
    private val cipher: BlobCipher,
) : SignedPreKeyStore {
    override fun loadSignedPreKey(id: Int): SignedPreKeyRecord {
        val e = dao.get(id) ?: throw InvalidKeyIdException("No signed prekey $id")
        return SignedPreKeyRecord(cipher.decrypt(e.recordEnc))
    }
    override fun loadSignedPreKeys(): MutableList<SignedPreKeyRecord> =
        dao.all().map { SignedPreKeyRecord(cipher.decrypt(it.recordEnc)) }.toMutableList()
    override fun storeSignedPreKey(id: Int, record: SignedPreKeyRecord) {
        dao.upsert(SignedPreKeyEntity(id, cipher.encrypt(record.serialize()), active = true))
    }
    override fun containsSignedPreKey(id: Int): Boolean = dao.count(id) > 0
    override fun removeSignedPreKey(id: Int) = dao.delete(id)
}
