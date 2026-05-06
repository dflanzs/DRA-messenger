@file:Suppress("unused")

package com.example.mobile_app.domain.signal

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import org.json.JSONArray
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyType
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.util.KeyHelper
import kotlinx.coroutines.flow.first

private const val PREFS_NAME = "signal_store"

private val Context.signalDataStore by preferencesDataStore(name = PREFS_NAME)

private val REGISTRATION_ID_KEY = intPreferencesKey("registration_id")
private val IDENTITY_KEY_PAIR_B64_KEY = stringPreferencesKey("identity_key_pair_b64")
private val SIGNED_PRE_KEY_RECORD_B64_KEY = stringPreferencesKey("signed_pre_key_record_b64")
private val KYBER_PRE_KEY_RECORD_B64_KEY = stringPreferencesKey("kyber_pre_key_record_b64")
private val ONE_TIME_PRE_KEY_RECORDS_B64_KEY = stringPreferencesKey("one_time_pre_key_records_b64")
private val ACTIVE_SIGNED_PRE_KEY_ID_KEY = intPreferencesKey("active_signed_pre_key_id")
private val ACTIVE_KYBER_PRE_KEY_ID_KEY = intPreferencesKey("active_kyber_pre_key_id")
private val ONE_TIME_PRE_KEYS_STORED_KEY = intPreferencesKey("one_time_pre_keys_stored")

class SignalStore(
    private val context: Context,
) {

    suspend fun initializeIfNeeded() {
        val prefs = context.signalDataStore.data.first()
        if (prefs[REGISTRATION_ID_KEY] == null) {
            val material = generateBootstrapMaterial()
            storeBootstrapMaterial(material)
            updateBootstrapInfo(
                activeSignedPreKeyId = material.signedPreKeyRecord.id,
                activeKyberPreKeyId = material.kyberPreKeyRecord.id,
                oneTimePreKeysStored = material.oneTimePreKeyRecords.size,
            )
        }
    }

    suspend fun getRegistrationId(): Int {
        val prefs = context.signalDataStore.data.first()
        return prefs[REGISTRATION_ID_KEY] ?: throw IllegalStateException("Registration ID not initialized")
    }

    suspend fun loadOrCreateBootstrapMaterial(): SignalBootstrapMaterial {
        val existing = loadBootstrapMaterial()
        if (existing != null) return existing

        val material = generateBootstrapMaterial()
        storeBootstrapMaterial(material)
        updateBootstrapInfo(
            activeSignedPreKeyId = material.signedPreKeyRecord.id,
            activeKyberPreKeyId = material.kyberPreKeyRecord.id,
            oneTimePreKeysStored = material.oneTimePreKeyRecords.size,
        )
        return material
    }

    suspend fun storeBootstrapMaterial(material: SignalBootstrapMaterial) {
        context.signalDataStore.edit { prefs ->
            prefs[REGISTRATION_ID_KEY] = material.registrationId
            prefs[IDENTITY_KEY_PAIR_B64_KEY] = material.identityKeyPair.serialize().b64()
            prefs[SIGNED_PRE_KEY_RECORD_B64_KEY] = material.signedPreKeyRecord.serialize().b64()
            prefs[KYBER_PRE_KEY_RECORD_B64_KEY] = material.kyberPreKeyRecord.serialize().b64()
            prefs[ONE_TIME_PRE_KEY_RECORDS_B64_KEY] =
                JSONArray(material.oneTimePreKeyRecords.map { it.serialize().b64() }).toString()
        }
    }

    suspend fun loadBootstrapMaterial(): SignalBootstrapMaterial? {
        val prefs = context.signalDataStore.data.first()
        val registrationId = prefs[REGISTRATION_ID_KEY] ?: return null
        val identityKeyPairB64 = prefs[IDENTITY_KEY_PAIR_B64_KEY] ?: return null
        val signedPreKeyRecordB64 = prefs[SIGNED_PRE_KEY_RECORD_B64_KEY] ?: return null
        val kyberPreKeyRecordB64 = prefs[KYBER_PRE_KEY_RECORD_B64_KEY] ?: return null
        val oneTimePreKeyRecordsB64 = prefs[ONE_TIME_PRE_KEY_RECORDS_B64_KEY] ?: return null

        val oneTimePreKeyRecords = JSONArray(oneTimePreKeyRecordsB64).let { jsonArray ->
            buildList {
                for (i in 0 until jsonArray.length()) {
                    add(PreKeyRecord(jsonArray.getString(i).fromB64()))
                }
            }
        }

        return SignalBootstrapMaterial(
            registrationId = registrationId,
            identityKeyPair = IdentityKeyPair(identityKeyPairB64.fromB64()),
            signedPreKeyRecord = SignedPreKeyRecord(signedPreKeyRecordB64.fromB64()),
            kyberPreKeyRecord = KyberPreKeyRecord(kyberPreKeyRecordB64.fromB64()),
            oneTimePreKeyRecords = oneTimePreKeyRecords,
        )
    }

    suspend fun updateBootstrapInfo(
        activeSignedPreKeyId: Int,
        activeKyberPreKeyId: Int,
        oneTimePreKeysStored: Int,
    ) {
        context.signalDataStore.edit { prefs ->
            prefs[ACTIVE_SIGNED_PRE_KEY_ID_KEY] = activeSignedPreKeyId
            prefs[ACTIVE_KYBER_PRE_KEY_ID_KEY] = activeKyberPreKeyId
            prefs[ONE_TIME_PRE_KEYS_STORED_KEY] = oneTimePreKeysStored
        }
    }

    suspend fun getBootstrapInfo(): BootstrapInfo {
        val prefs = context.signalDataStore.data.first()
        return BootstrapInfo(
            activeSignedPreKeyId = prefs[ACTIVE_SIGNED_PRE_KEY_ID_KEY] ?: 0,
            activeKyberPreKeyId = prefs[ACTIVE_KYBER_PRE_KEY_ID_KEY] ?: 0,
            oneTimePreKeysStored = prefs[ONE_TIME_PRE_KEYS_STORED_KEY] ?: 0,
        )
    }

    private fun generateBootstrapMaterial(): SignalBootstrapMaterial {
        val registrationId = KeyHelper.generateRegistrationId(false)
        val identityKeyPair = IdentityKeyPair.generate()
        val signedPreKeyPair = ECKeyPair.generate()
        val signedPreKeyRecord = SignedPreKeyRecord(
            1,
            System.currentTimeMillis(),
            signedPreKeyPair,
            identityKeyPair.privateKey.calculateSignature(signedPreKeyPair.publicKey.serialize()),
        )
        val kyberPreKeyPair = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
        val kyberPreKeyRecord = KyberPreKeyRecord(
            1,
            System.currentTimeMillis(),
            kyberPreKeyPair,
            identityKeyPair.privateKey.calculateSignature(kyberPreKeyPair.publicKey.serialize()),
        )
        val oneTimePreKeyRecords = (1..100).map { id ->
            PreKeyRecord(id, ECKeyPair.generate())
        }

        return SignalBootstrapMaterial(
            registrationId = registrationId,
            identityKeyPair = identityKeyPair,
            signedPreKeyRecord = signedPreKeyRecord,
            kyberPreKeyRecord = kyberPreKeyRecord,
            oneTimePreKeyRecords = oneTimePreKeyRecords,
        )
    }

    private fun String.fromB64(): ByteArray = android.util.Base64.decode(this, android.util.Base64.NO_WRAP)
    private fun ByteArray.b64(): String = android.util.Base64.encodeToString(this, android.util.Base64.NO_WRAP)
}

data class SignalBootstrapMaterial(
    val registrationId: Int,
    val identityKeyPair: IdentityKeyPair,
    val signedPreKeyRecord: SignedPreKeyRecord,
    val kyberPreKeyRecord: KyberPreKeyRecord,
    val oneTimePreKeyRecords: List<PreKeyRecord>,
)

data class BootstrapInfo(
    val activeSignedPreKeyId: Int,
    val activeKyberPreKeyId: Int,
    val oneTimePreKeysStored: Int,
)


