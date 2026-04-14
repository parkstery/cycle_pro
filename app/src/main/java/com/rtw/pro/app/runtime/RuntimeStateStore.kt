package com.rtw.pro.app.runtime

import com.rtw.pro.baseline.ui.streetview.StreetViewMode
import com.rtw.pro.map.data.MapBindErrorCode

data class RuntimeState(
    val authReady: Boolean = false,
    val authStatus: String = "INIT",
    val authMessage: String = "",
    val mapReady: Boolean = false,
    val mapMode: StreetViewMode = StreetViewMode.MAP_ONLY,
    val mapMessage: String = "",
    val mapErrorCode: MapBindErrorCode? = null,
    val pushTokenSynced: Boolean = false
)

class RuntimeStateStore {
    private var state: RuntimeState = RuntimeState()

    fun get(): RuntimeState = state

    fun updateAuthReady(ready: Boolean) {
        state = state.copy(authReady = ready)
    }

    fun updateAuthUi(status: String, message: String) {
        state = state.copy(
            authStatus = status,
            authMessage = message
        )
    }

    fun updateMapReady(ready: Boolean) {
        state = state.copy(mapReady = ready)
    }

    fun updateMapUi(
        mode: StreetViewMode,
        message: String,
        errorCode: MapBindErrorCode?
    ) {
        state = state.copy(
            mapMode = mode,
            mapMessage = message,
            mapErrorCode = errorCode
        )
    }

    fun updatePushTokenSynced(synced: Boolean) {
        state = state.copy(pushTokenSynced = synced)
    }
}
