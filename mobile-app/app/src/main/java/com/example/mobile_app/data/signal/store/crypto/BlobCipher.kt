package com.example.mobile_app.data.signal.store.crypto

/**
 * Cifra/descifra blobs sensibles antes de persistirlos.
 * Implementación real: Android Keystore (AES/GCM). En tests: un fake determinista.
 */
interface BlobCipher {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(payload: ByteArray): ByteArray
}
