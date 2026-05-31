package com.example.mobile_app.signal

import com.example.mobile_app.data.signal.store.crypto.BlobCipher

/** Fake determinista: prefija un byte marcador y aplica XOR. Verifica que el store cifra/descifra de verdad. */
class FakeBlobCipher(private val key: Byte = 0x5A) : BlobCipher {
    override fun encrypt(plain: ByteArray): ByteArray =
        ByteArray(plain.size + 1).also {
            it[0] = MARKER
            for (i in plain.indices) it[i + 1] = (plain[i].toInt() xor key.toInt()).toByte()
        }

    override fun decrypt(payload: ByteArray): ByteArray {
        require(payload.isNotEmpty() && payload[0] == MARKER) { "blob no cifrado por FakeBlobCipher" }
        return ByteArray(payload.size - 1) { (payload[it + 1].toInt() xor key.toInt()).toByte() }
    }

    companion object { private const val MARKER: Byte = 0x7E }
}
