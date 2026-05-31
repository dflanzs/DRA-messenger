package com.example.mobile_app.domain.signal

/** Una SKDM 1:1 lista para enviar a un miembro concreto (el llamante la manda por sendPrivateMessage). */
data class SkdmOut(val recipientUserId: Long, val payload: EncryptedPayload)

/** Ciphertext de grupo listo para el wire (un solo blob; el backend lo hace fanout a los miembros). */
data class GroupMessageOut(val payload: EncryptedPayload) {
    val cypherTextType: Short get() = payload.type
    val cypherTextB64: String get() = payload.b64()
}
