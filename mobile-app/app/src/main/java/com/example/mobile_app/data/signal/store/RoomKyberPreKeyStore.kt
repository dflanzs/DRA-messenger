package com.example.mobile_app.data.signal.store

import com.example.mobile_app.data.signal.store.crypto.BlobCipher
import com.example.mobile_app.data.signal.store.db.KyberPreKeyDao
import com.example.mobile_app.data.signal.store.db.KyberPreKeyEntity
import org.signal.libsignal.protocol.InvalidKeyIdException
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.KyberPreKeyStore

class RoomKyberPreKeyStore(
    private val dao: KyberPreKeyDao,
    private val cipher: BlobCipher,
) : KyberPreKeyStore {
    override fun loadKyberPreKey(id: Int): KyberPreKeyRecord {
        val e = dao.get(id) ?: throw InvalidKeyIdException("No kyber prekey $id")
        return KyberPreKeyRecord(cipher.decrypt(e.recordEnc))
    }
    override fun loadKyberPreKeys(): MutableList<KyberPreKeyRecord> =
        dao.all().map { KyberPreKeyRecord(cipher.decrypt(it.recordEnc)) }.toMutableList()
    override fun storeKyberPreKey(id: Int, record: KyberPreKeyRecord) {
        dao.upsert(KyberPreKeyEntity(id, cipher.encrypt(record.serialize()), active = true))
    }
    override fun containsKyberPreKey(id: Int): Boolean = dao.count(id) > 0
    // La kyber prekey firmada se reutiliza (no es one-time en esta implementación): no-op.
    override fun markKyberPreKeyUsed(kyberPreKeyId: Int, signedPreKeyId: Int, baseKey: ECPublicKey) {
        /* no-op intencional */
    }
}
