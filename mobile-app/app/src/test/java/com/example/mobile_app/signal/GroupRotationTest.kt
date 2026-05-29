package com.example.mobile_app.signal

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroupRotationTest {

    @Test fun removedMemberCannotDecryptAfterRotation() = runTest {
        val support = GroupTestSupport()
        val alice = support.svc(1); val bob = support.svc(2); val carol = support.svc(3)
        val groupId = 200L

        // Distribución inicial a Bob y Carol.
        support.distribute(alice, 1, listOf(2L to bob, 3L to carol), groupId)
        val before = alice.encryptGroup(groupId, "antes".toByteArray())
        assertEquals("antes", String(carol.decryptGroup(groupId, 1L, before.payload)))

        // Carol sale -> Alice rota su sender key y redistribuye solo a Bob.
        alice.rotateSenderKey(groupId)
        support.distribute(alice, 1, listOf(2L to bob), groupId)
        val after = alice.encryptGroup(groupId, "despues".toByteArray())

        // Bob (sigue) descifra; Carol (saliente) no obtiene el plaintext nuevo.
        assertEquals("despues", String(bob.decryptGroup(groupId, 1L, after.payload)))
        val carolPlain = String(carol.decryptGroup(groupId, 1L, after.payload))
        assertNotEquals("despues", carolPlain)
    }
}
