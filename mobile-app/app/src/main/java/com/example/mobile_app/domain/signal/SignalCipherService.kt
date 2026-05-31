package com.example.mobile_app.domain.signal

import android.util.Base64
import com.example.mobile_app.data.model.signal.SignalBundleResponseDto
import com.example.mobile_app.data.signal.store.PersistentSignalProtocolStore
import com.example.mobile_app.data.signal.store.db.SenderKeyDistributionEntity
import com.example.mobile_app.data.signal.store.key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.NoSessionException
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.SessionCipher
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.UntrustedIdentityException
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.groups.GroupCipher
import org.signal.libsignal.protocol.groups.GroupSessionBuilder
import org.signal.libsignal.protocol.kem.KEMPublicKey
import org.signal.libsignal.protocol.message.CiphertextMessage
import org.signal.libsignal.protocol.message.PreKeySignalMessage
import org.signal.libsignal.protocol.message.SenderKeyDistributionMessage
import org.signal.libsignal.protocol.message.SignalMessage
import org.signal.libsignal.protocol.state.IdentityKeyStore
import org.signal.libsignal.protocol.state.PreKeyBundle
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Tipo y bytes del ciphertext, listos para el wire (cypherTextType / cypherTextB64). */
data class EncryptedPayload(val type: Short, val bytes: ByteArray) {
    fun b64(): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
}

class IdentityChangedException(val address: String) : Exception("Identidad cambiada: $address")

/** Resuelve el prekey bundle de un usuario (normalmente vía SignalRepository.getUserBundle). */
fun interface BundleFetcher { suspend fun fetch(remoteUserId: Long): SignalBundleResponseDto }

class SignalCipherService(
    private val store: PersistentSignalProtocolStore,
    private val ownUserId: () -> Long,
    private val bundleFetcher: BundleFetcher,
) {
    private val mutexes = HashMap<String, Mutex>()
    @Synchronized private fun mutexFor(key: String) = mutexes.getOrPut(key) { Mutex() }

    private fun address(userId: Long) = SignalProtocolAddress(userId.toString(), 1)
    private fun b64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)

    suspend fun encryptDirect(remoteUserId: Long, plaintext: ByteArray): EncryptedPayload =
        withContext(Dispatchers.IO) {
            val addr = address(remoteUserId)
            mutexFor(addr.key()).withLock {
                if (!store.containsSession(addr)) establishSession(remoteUserId, addr)
                val cipher = SessionCipher(store, addr)
                val msg: CiphertextMessage = cipher.encrypt(plaintext)
                EncryptedPayload(msg.type.toShort(), msg.serialize())
            }
        }

    suspend fun decryptDirect(remoteUserId: Long, payload: EncryptedPayload): ByteArray =
        withContext(Dispatchers.IO) {
            val addr = address(remoteUserId)
            mutexFor(addr.key()).withLock {
                val cipher = SessionCipher(store, addr)
                when (payload.type.toInt()) {
                    CiphertextMessage.PREKEY_TYPE ->
                        cipher.decrypt(PreKeySignalMessage(payload.bytes))
                    else ->
                        cipher.decrypt(SignalMessage(payload.bytes))
                }
            }
        }

    private suspend fun establishSession(remoteUserId: Long, addr: SignalProtocolAddress) {
        val b = bundleFetcher.fetch(remoteUserId)
        val identityKey = IdentityKey(b64(b.identityKeyPublicB64))
        // TOFU: si la identidad difiere de la confiada, abortar.
        if (!store.isTrustedIdentity(addr, identityKey, IdentityKeyStore.Direction.SENDING)) {
            throw IdentityChangedException(addr.key())
        }
        val hasOtp = b.oneTimePreKeyId != null && b.oneTimePreKeyPublicB64 != null
        val bundle = PreKeyBundle(
            b.registrationId,
            b.deviceId,
            if (hasOtp) b.oneTimePreKeyId!! else PreKeyBundle.NULL_PRE_KEY_ID,
            if (hasOtp) ECPublicKey(b64(b.oneTimePreKeyPublicB64!!)) else null,
            b.signedPreKeyId,
            ECPublicKey(b64(b.signedPreKeyPublicB64)),
            b64(b.signedPreKeySignatureB64),
            identityKey,
            b.kyberPreKeyId,
            KEMPublicKey(b64(b.kyberPreKeyPublicB64)),
            b64(b.kyberPreKeySignatureB64),
        )
        try {
            SessionBuilder(store, addr).process(bundle)
        } catch (e: UntrustedIdentityException) {
            throw IdentityChangedException(addr.key())
        }
        store.saveIdentity(addr, identityKey)
    }

    // ---- Grupos (Sender Keys) ----

    private fun distributionIdFor(groupId: Long): UUID =
        UUID.nameUUIDFromBytes("group:$groupId".toByteArray())

    private fun ownAddress() = SignalProtocolAddress(ownUserId().toString(), 1)

    // Mensajes de grupo recibidos antes de tener la sender key del emisor (cola en memoria).
    private val pendingGroup = ConcurrentHashMap<String, MutableList<EncryptedPayload>>()
    private fun pendKey(groupId: Long, senderUserId: Long) = "$groupId:$senderUserId"

    /**
     * Garantiza que nuestra sender key del grupo se ha distribuido a [members].
     * Devuelve las SKDM 1:1 que el llamante debe enviar (vacío si ya estaban distribuidas).
     */
    suspend fun ensureSenderKeyDistributed(groupId: Long, members: List<Long>): List<SkdmOut> =
        withContext(Dispatchers.IO) {
            val dist = distributionIdFor(groupId)
            // create() genera/recupera nuestra sender key para (ownAddress, dist) y produce su SKDM.
            val skdm = GroupSessionBuilder(store.senderKeys).create(ownAddress(), dist)
            val skdmBytes = skdm.serialize()
            val distDao = store.db.senderKeyDistributionDao()
            val out = ArrayList<SkdmOut>()
            for (member in members) {
                if (member == ownUserId()) continue
                if (distDao.get(dist.toString(), member)?.sent == true) continue
                val frame = DirectFrame.skdm(groupId, skdmBytes).encode()
                out += SkdmOut(member, encryptDirect(member, frame))
                distDao.upsert(SenderKeyDistributionEntity("$dist|$member", dist.toString(), member, true))
            }
            out
        }

    /** Procesa una SKDM recibida de [senderUserId] para [groupId] (registra su sender key). */
    suspend fun processSenderKeyDistribution(senderUserId: Long, groupId: Long, skdmBytes: ByteArray) =
        withContext(Dispatchers.IO) {
            val sender = SignalProtocolAddress(senderUserId.toString(), 1)
            GroupSessionBuilder(store.senderKeys).process(sender, SenderKeyDistributionMessage(skdmBytes))
        }

    /** Cifra un mensaje de grupo (un solo blob para fanout del backend). */
    suspend fun encryptGroup(groupId: Long, plaintext: ByteArray): GroupMessageOut =
        withContext(Dispatchers.IO) {
            val dist = distributionIdFor(groupId)
            val msg = GroupCipher(store.senderKeys, ownAddress()).encrypt(dist, plaintext)
            GroupMessageOut(EncryptedPayload(msg.type.toShort(), msg.serialize()))
        }

    /** Descifra un mensaje de grupo de [senderUserId]. Si falta la sender key, encola y devuelve vacío. */
    suspend fun decryptGroup(groupId: Long, senderUserId: Long, payload: EncryptedPayload): ByteArray =
        withContext(Dispatchers.IO) {
            val sender = SignalProtocolAddress(senderUserId.toString(), 1)
            try {
                GroupCipher(store.senderKeys, sender).decrypt(payload.bytes)
            } catch (e: NoSessionException) {
                pendingGroup.getOrPut(pendKey(groupId, senderUserId)) { mutableListOf() }.add(payload)
                ByteArray(0)
            }
        }

    /** Reintenta los mensajes de grupo encolados de [senderUserId] tras recibir su SKDM. */
    suspend fun drainPendingGroup(groupId: Long, senderUserId: Long): List<ByteArray> =
        withContext(Dispatchers.IO) {
            val queued = pendingGroup.remove(pendKey(groupId, senderUserId)) ?: return@withContext emptyList()
            val cipher = GroupCipher(store.senderKeys, SignalProtocolAddress(senderUserId.toString(), 1))
            queued.mapNotNull { runCatching { cipher.decrypt(it.bytes) }.getOrNull() }
        }

    /** Rota nuestra sender key del grupo (al salir/expulsar un miembro): se redistribuye en el próximo envío. */
    suspend fun rotateSenderKey(groupId: Long) = withContext(Dispatchers.IO) {
        val dist = distributionIdFor(groupId)
        val addr = ownAddress()
        store.senderKeyDao().delete("${addr.name}:${addr.deviceId}|$dist")
        store.db.senderKeyDistributionDao().clearForDistribution(dist.toString())
    }
}

fun EncryptedPayload.toWireB64(): Pair<Short, String> = type to b64()

fun payloadFromWire(cypherTextType: Short, cypherTextB64: String): EncryptedPayload =
    EncryptedPayload(cypherTextType, Base64.decode(cypherTextB64, Base64.NO_WRAP))
