package com.example.mobile_app.signal

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FakeBlobCipherTest {
    @Test fun roundTrip() {
        val c = FakeBlobCipher()
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val enc = c.encrypt(data)
        assertFalse("el cifrado no debe igualar el plano", enc.contentEquals(data))
        assertArrayEquals(data, c.decrypt(enc))
    }
}
