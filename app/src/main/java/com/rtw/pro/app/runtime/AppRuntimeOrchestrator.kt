package com.rtw.pro.app.runtime

import com.rtw.pro.foundation.data.auth.AuthRuntimeCoordinator
import com.rtw.pro.foundation.data.auth.AuthRuntimeStatus
import com.rtw.pro.map.data.MapProviderConfig
import com.rtw.pro.map.data.StreetViewProviderConfig
import com.rtw.pro.map.domain.MapRuntimeOrchestrator
import com.rtw.pro.notification.data.FcmTopicSubscriptionManager
import com.rtw.pro.notification.data.FcmTokenSyncCoordinator
import com.rtw.pro.notification.domain.PushUiMessagePolicy

class AppRuntimeOrchestrator(
    private val authCoordinator: AuthRuntimeCoordinator,
    private val mapRuntimeOrchestrator: MapRuntimeOrchestrator,
    private val tokenSyncCoordinator: FcmTokenSyncCoordinator,
    private val topicSubscriptionManager: FcmTopicSubscriptionManager,
    private val stateStore: RuntimeStateStore
) {
    private fun updateMapAndPushState(
        mapConfig: MapProviderConfig,
        streetViewConfig: StreetViewProviderConfig
    ) {
        val map = mapRuntimeOrchestrator.prepare(mapConfig, streetViewConfig)
        stateStore.updateMapReady(map.ready)
        stateStore.updateMapUi(
            mode = map.streetViewMode,
            message = map.message,
            errorCode = map.errorCode
        )

        val push = tokenSyncCoordinator.syncCurrentToken()
        stateStore.updatePushTokenUi(
            synced = push.success,
            errorCode = push.errorCode,
            message = PushUiMessagePolicy.tokenSyncMessage(push.errorCode)
        )
    }

    private fun updateAuthState(status: AuthRuntimeStatus, message: String) {
        val authReady = status == AuthRuntimeStatus.READY_WITH_SESSION ||
            status == AuthRuntimeStatus.READY_AFTER_SIGN_IN
        stateStore.updateAuthReady(authReady)
        stateStore.updateAuthUi(
            status = status.name,
            message = message
        )
    }

    fun onAppLaunch(
        mapConfig: MapProviderConfig,
        streetViewConfig: StreetViewProviderConfig
    ): RuntimeState {
        val auth = authCoordinator.initialize()
        updateAuthState(auth.status, auth.message)
        updateMapAndPushState(mapConfig, streetViewConfig)

        return stateStore.get()
    }

    fun refreshMapAndPush(
        mapConfig: MapProviderConfig,
        streetViewConfig: StreetViewProviderConfig
    ): RuntimeState {
        updateMapAndPushState(mapConfig, streetViewConfig)
        return stateStore.get()
    }

    fun retryAuthSignIn(): RuntimeState {
        val auth = authCoordinator.retrySignIn()
        updateAuthState(auth.status, auth.message)
        return stateStore.get()
    }

    fun signOutAuth(): RuntimeState {
        val auth = authCoordinator.signOut()
        updateAuthState(auth.status, auth.message)
        return stateStore.get()
    }

    fun onFcmTokenRefreshed(newToken: String): RuntimeState {
        val result = tokenSyncCoordinator.onTokenRefreshed(newToken)
        stateStore.updatePushTokenUi(
            synced = result.success,
            errorCode = result.errorCode,
            message = PushUiMessagePolicy.tokenSyncMessage(result.errorCode)
        )
        return stateStore.get()
    }

    fun retryPushTokenSync(): RuntimeState {
        val result = tokenSyncCoordinator.syncCurrentToken()
        stateStore.updatePushTokenUi(
            synced = result.success,
            errorCode = result.errorCode,
            message = PushUiMessagePolicy.tokenSyncMessage(result.errorCode)
        )
        return stateStore.get()
    }

    fun subscribeEventTopic(topic: String): RuntimeState {
        val result = topicSubscriptionManager.subscribeEventTopicDetailed(topic)
        stateStore.updatePushTopicUi(
            subscribed = result.success,
            topic = result.topic,
            errorCode = result.errorCode,
            message = PushUiMessagePolicy.topicSubscriptionMessage(
                errorCode = result.errorCode,
                subscribed = result.success
            )
        )
        return stateStore.get()
    }
}
