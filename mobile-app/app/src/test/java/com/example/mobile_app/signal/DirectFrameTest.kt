package com.example.mobile_app.signal

import com.example.mobile_app.domain.signal.DirectFrame
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectFrameTest {
    @Test fun textFrameRoundTrip() {
        val f = DirectFrame.text("hola".toByteArray())
        val dec = DirectFrame.decode(f.encode())
        assertTrue(dec is DirectFrame.Text)
        assertArrayEquals("hola".toByteArray(), (dec as DirectFrame.Text).body)
    }

    @Test fun skdmFrameRoundTrip() {
        val f = DirectFrame.skdm(groupId = 42L, skdmBytes = byteArrayOf(9, 9, 9))
        val dec = DirectFrame.decode(f.encode())
        assertTrue(dec is DirectFrame.Skdm)
        dec as DirectFrame.Skdm
        assertEquals(42L, dec.groupId)
        assertArrayEquals(byteArrayOf(9, 9, 9), dec.skdmBytes)
    }
}
