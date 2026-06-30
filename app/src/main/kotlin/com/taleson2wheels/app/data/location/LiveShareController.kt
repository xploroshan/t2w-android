package com.taleson2wheels.app.data.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide bridge between [LiveLocationService] (which streams GPS in the
 * background) and the UI. The service publishes which ride is being shared and
 * how many fixes have been accepted; the live screen observes it so the
 * "Sharing live" state survives navigation and screen-off.
 */
object LiveShareController {

    private val _activeRideId = MutableStateFlow<String?>(null)
    val activeRideId: StateFlow<String?> = _activeRideId.asStateFlow()

    private val _uploaded = MutableStateFlow(0)
    val uploaded: StateFlow<Int> = _uploaded.asStateFlow()

    fun isSharing(rideId: String): Boolean = _activeRideId.value == rideId

    internal fun onStart(rideId: String) {
        _activeRideId.value = rideId
        _uploaded.value = 0
    }

    internal fun onUploaded(accepted: Int) {
        _uploaded.value += accepted
    }

    internal fun onStop() {
        _activeRideId.value = null
    }
}
