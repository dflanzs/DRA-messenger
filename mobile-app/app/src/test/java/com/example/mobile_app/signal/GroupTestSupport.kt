package com.example.mobile_app.signal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.mobile_app.data.model.signal.SignalBundleResponseDto
import com.example.mobile_app.data.signal.store.PersistentSignalProtocolStore
import com.example.mobile_app.data.signal.store.SignalStoreBootstrap
import com.example.mobile_app.data.signal.store.db.SignalDatabase
import com.example.mobile_app.domain.signal.DirectFrame
import com.example.mobile_app.domain.signal.IdentityEventBus
import com.example.mobile_app.domain.signal.SignalCipherService
import android.util.Base64

/** Soporte para tests de grupo: peers con store propio y entrega de SKDM por el canal 1:1 simulado. */
class GroupTestSupport {
    private inner class Peer(val userId: Long) {
        val db: SignalDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SignalDatabase::class.java,
        ).allowMainThreadQueries().build()
        val store = PersistentSignalProtocolStore(db, FakeBlobCipher(), IdentityEventBus())
        init { SignalStoreBootstrap(store, db, oneTimeCount = 10).initializeIfNeeded() }
    }

    private val peers = HashMap<Long, Peer>()
    private fun peer(id: Long) = peers.getOrPut(id) { Peer(id) }

    private fun bundleOf(p: Peer): SignalBundleResponseDto {
        val s = p.store.loadSignedPreKey(1); val k = p.store.loadKyberPreKey(1); val o = p.store.loadPreKey(1)
        fun ByteArray.b64() = Base64.encodeToString(this, Base64.NO_WRAP)
        return SignalBundleResponseDto(
            p.store.localRegistrationId, 1, p.store.identityKeyPair.publicKey.serialize().b64(),
            1, s.keyPair.publicKey.serialize().b64(), s.signature.b64(),
            1, k.keyPair.publicKey.serialize().b64(), k.signature.b64(),
            1, o.keyPair.publicKey.serialize().b64(),
        )
    }

    fun svc(userId: Long): SignalCipherService {
        val self = peer(userId)
        return SignalCipherService(self.store, ownUserId = { userId }) { bundleOf(peer(it)) }
    }

    /** Distribuye la SKDM de [fromUserId] a cada [recipients] y la procesa en cada receptor. */
    suspend fun distribute(
        from: SignalCipherService,
        fromUserId: Long,
        recipients: List<Pair<Long, SignalCipherService>>,
        groupId: Long,
    ) {
        val skdms = from.ensureSenderKeyDistributed(groupId, recipients.map { it.first })
        skdms.forEach { out ->
            val recipientSvc = recipients.first { it.first == out.recipientUserId }.second
            val frameBytes = recipientSvc.decryptDirect(remoteUserId = fromUserId, out.payload)
            val frame = DirectFrame.decode(frameBytes) as DirectFrame.Skdm
            recipientSvc.processSenderKeyDistribution(fromUserId, frame.groupId, frame.skdmBytes)
        }
    }
}
