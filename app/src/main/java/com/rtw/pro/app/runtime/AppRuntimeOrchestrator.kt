package com.rtw.pro.app.runtime

import com.rtw.pro.foundation.data.auth.AuthRuntimeCoordinator
import com.rtw.pro.foundation.data.auth.AuthRuntimeStatus
import com.rtw.pro.map.data.MapProviderConfig
import com.rtw.pro.map.data.StreetViewProviderConfig
import com.rtw.pro.map.domain.MapRuntimeOrchestrator
import com.rtw.pro.notification.data.FcmTokenSyncCoordinator

class AppRuntimeOrchestrator(
    private val authCoordinator: AuthRuntimeCoordinator,
    private val mapRuntimeOrchestrator: MapRuntimeOrchestrator,
    private val tokenSyncCoordinator: FcmTokenSyncCoordinator,
    private val stateStore: RuntimeStateStore
) {
    fun onAppLaunch(
        mapConfig: MapProviderConfig,
        streetViewConfig: StreetViewProviderConfig
    ): RuntimeState {
        val auth = authCoordinator.initialize()
        val authReady = auth.status == AuthRuntimeStatus.READY_WITH_SESSION ||
            auth.status == AuthRuntimeStatus.READY_AFTER_SIGN_IN
        stateStore.updateAuthReady(authReady)
        stateStore.updateAuthUi(
            status = auth.status.name,
            message = auth.message
        )

        val map = mapRuntimeOrchestrator.prepare(mapConfig, streetViewConfig)
        stateStore.updateMapReady(map.ready)
        stateStore.updateMapUi(
            mode = map.streetViewMode,
            message = map.message,
            errorCode = map.errorCode
        )

        val push = tokenSyncCoordinator.syncCurrentToken()
        stateStore.updatePushTokenSynced(push.success)

        return stateStore.get()
    }

    fun onFcmTokenRefreshed(newToken: String): RuntimeState {
        val result = tokenSyncCoordinator.onTokenRefreshed(newToken)
        stateStore.updatePushTokenSynced(result.success)
        return stateStore.get()
    }
}
