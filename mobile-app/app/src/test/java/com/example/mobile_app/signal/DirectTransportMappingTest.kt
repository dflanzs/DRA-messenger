package com.example.mobile_app.signal

import com.example.mobile_app.domain.signal.EncryptedPayload
import com.example.mobile_app.domain.signal.payloadFromWire
import com.example.mobile_app.domain.signal.toWireB64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DirectTransportMappingTest {
    @Test fun payloadToWireAndBack() {
        val p = EncryptedPayload(type = 3, bytes = byteArrayOf(10, 20, 30))
        val (type, b64) = p.toWireB64()
        assertEquals(3.toShort(), type)
        val back = payloadFromWire(type, b64)
        assertEquals(3.toShort(), back.type)
        assertArrayEquals(byteArrayOf(10, 20, 30), back.bytes)
    }
}
