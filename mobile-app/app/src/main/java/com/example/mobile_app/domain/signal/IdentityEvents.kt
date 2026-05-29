package com.example.mobile_app.domain.signal

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Evento emitido cuando un contacto presenta una identity key distinta a la confiada (posible MITM). */
data class IdentityChanged(val address: String)

class IdentityEventBus {
    private val _events = MutableSharedFlow<IdentityChanged>(extraBufferCapacity = 16)
    val events: SharedFlow<IdentityChanged> = _events
    fun emitChange(address: String) { _events.tryEmit(IdentityChanged(address)) }
}
