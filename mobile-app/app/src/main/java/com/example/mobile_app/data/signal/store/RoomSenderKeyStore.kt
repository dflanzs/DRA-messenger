package com.example.mobile_app.data.signal.store

import com.example.mobile_app.data.signal.store.crypto.BlobCipher
import com.example.mobile_app.data.signal.store.db.SenderKeyDao
import com.example.mobile_app.data.signal.store.db.SenderKeyEntity
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.groups.state.SenderKeyStore
import java.util.UUID

class RoomSenderKeyStore(
    private val dao: SenderKeyDao,
    private val cipher: BlobCipher,
) : SenderKeyStore {

    private fun keyOf(sender: SignalProtocolAddress, distributionId: UUID): String =
        "${sender.name}:${sender.deviceId}|$distributionId"

    override fun storeSenderKey(sender: SignalProtocolAddress, distributionId: UUID, record: SenderKeyRecord) {
        dao.upsert(SenderKeyEntity(keyOf(sender, distributionId), cipher.encrypt(record.serialize())))
    }

    // SenderKeyRecord no tiene constructor vacío: devolvemos null si no hay registro (libsignal lo maneja).
    override fun loadSenderKey(sender: SignalProtocolAddress, distributionId: UUID): SenderKeyRecord? {
        val e = dao.get(keyOf(sender, distributionId)) ?: return null
        return SenderKeyRecord(cipher.decrypt(e.recordEnc))
    }
}
