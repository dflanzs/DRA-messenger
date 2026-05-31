package com.example.mobile_app.signal

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroupCipherRoundTripTest {

    @Test fun groupFanoutDecryptsForAllMembers() = runTest {
        val support = GroupTestSupport()
        val alice = support.svc(1); val bob = support.svc(2); val carol = support.svc(3)
        val groupId = 100L

        // Alice distribuye su sender key a Bob y Carol.
        support.distribute(alice, fromUserId = 1, recipients = listOf(2L to bob, 3L to carol), groupId = groupId)

        // Alice cifra UN mensaje de grupo; ambos descifran el mismo blob.
        val out = alice.encryptGroup(groupId, "hola grupo".toByteArray())
        assertEquals("hola grupo", String(bob.decryptGroup(groupId, senderUserId = 1L, out.payload)))
        assertEquals("hola grupo", String(carol.decryptGroup(groupId, senderUserId = 1L, out.payload)))
    }

    @Test fun lateMemberStillDecryptsAfterReceivingSkdm() = runTest {
        val support = GroupTestSupport()
        val alice = support.svc(1); val bob = support.svc(2)
        val groupId = 101L

        support.distribute(alice, 1, listOf(2L to bob), groupId)
        val m1 = alice.encryptGroup(groupId, "uno".toByteArray())
        assertEquals("uno", String(bob.decryptGroup(groupId, 1L, m1.payload)))

        // Segundo envío: la sender key existente NO debe rotar (create reutiliza).
        val m2 = alice.encryptGroup(groupId, "dos".toByteArray())
        assertEquals("dos", String(bob.decryptGroup(groupId, 1L, m2.payload)))
    }
}
