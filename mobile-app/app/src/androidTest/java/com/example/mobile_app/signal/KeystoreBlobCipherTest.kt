package com.example.mobile_app.signal

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mobile_app.data.signal.store.crypto.KeystoreBlobCipher
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreBlobCipherTest {
    @Test fun roundTrip() {
        val c = KeystoreBlobCipher()
        val data = ByteArray(64) { it.toByte() }
        val enc = c.encrypt(data)
        assertFalse(enc.contentEquals(data))
        assertArrayEquals(data, c.decrypt(enc))
    }
}
