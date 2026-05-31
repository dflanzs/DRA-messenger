package com.example.mobile_app.data.signal.store

import com.example.mobile_app.data.signal.store.db.BootstrapMetaEntity
import com.example.mobile_app.data.signal.store.db.SignalDatabase
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyType
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.util.KeyHelper

/** Genera y persiste el material inicial (identidad, signed/kyber prekeys, one-time prekeys) una sola vez. */
class SignalStoreBootstrap(
    private val store: PersistentSignalProtocolStore,
    private val db: SignalDatabase,
    private val oneTimeCount: Int = 20,
) {
    fun initializeIfNeeded() {
        if (store.identity.hasOwnIdentity()) return

        val registrationId = KeyHelper.generateRegistrationId(false)
        val idPair = IdentityKeyPair.generate()
        store.identity.initializeOwnIdentity(idPair, registrationId)

        val signedPair = ECKeyPair.generate()
        val signed = SignedPreKeyRecord(1, System.currentTimeMillis(), signedPair,
            idPair.privateKey.calculateSignature(signedPair.publicKey.serialize()))
        store.storeSignedPreKey(1, signed)

        val kemPair = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
        val kyber = KyberPreKeyRecord(1, System.currentTimeMillis(), kemPair,
            idPair.privateKey.calculateSignature(kemPair.publicKey.serialize()))
        store.storeKyberPreKey(1, kyber)

        (1..oneTimeCount).forEach { id -> store.storePreKey(id, PreKeyRecord(id, ECKeyPair.generate())) }

        db.bootstrapMetaDao().upsert(
            BootstrapMetaEntity(id = 0, migrated = false, lastOneTimePreKeyId = oneTimeCount)
        )
    }
}
