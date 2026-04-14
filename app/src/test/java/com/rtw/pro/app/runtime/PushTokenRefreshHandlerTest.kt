package com.rtw.pro.app.runtime

import com.rtw.pro.foundation.data.auth.AuthProviderConfig
import com.rtw.pro.foundation.data.auth.AuthRuntimeCoordinator
import com.rtw.pro.foundation.domain.auth.AuthGateway
import com.rtw.pro.foundation.domain.auth.AuthResult
import com.rtw.pro.foundation.domain.auth.AuthSession
import com.rtw.pro.map.data.AndroidMapRuntimeBinder
import com.rtw.pro.map.data.GoogleMapSdkGateway
import com.rtw.pro.map.data.MapPermissionGateway
import com.rtw.pro.map.data.MapPermissionState
import com.rtw.pro.map.data.StreetViewSdkGateway
import com.rtw.pro.map.domain.MapRuntimeOrchestrator
import com.rtw.pro.notification.data.FcmSubscriptionClient
import com.rtw.pro.notification.data.FcmTopicSubscriptionManager
import com.rtw.pro.notification.data.FcmTokenProvider
import com.rtw.pro.notification.data.FcmTokenRegistrar
import com.rtw.pro.notification.data.FcmTokenSyncCoordinator
import kotlin.test.Test
import kotlin.test.assertTrue

class PushTokenRefreshHandlerTest {
    @Test
    fun onNewToken_updatesPushSyncState() {
        val orchestrator = AppRuntimeOrchestrator(
            authCoordinator = AuthRuntimeCoordinator(
                AuthProviderConfig("web", "project"),
                object : AuthGateway {
                    override fun getCurrentSession(): AuthSession? = AuthSession("u1", "t1")
                    override fun signInWithGoogle(): AuthResult = AuthResult.Success(AuthSession("u1", "t1"))
                }
            ),
            mapRuntimeOrchestrator = MapRuntimeOrchestrator(
                AndroidMapRuntimeBinder(
                    permissionGateway = object : MapPermissionGateway {
                        override fun locationPermissionState(): MapPermissionState = MapPermissionState.GRANTED
                    },
                    mapGateway = object : GoogleMapSdkGateway {
                        override fun initialize(apiKey: String): Boolean = true
                    },
                    streetViewGateway = object : StreetViewSdkGateway {
                        override fun initialize(timeoutMs: Long): Boolean = true
                    }
                )
            ),
            tokenSyncCoordinator = FcmTokenSyncCoordinator(
                provider = object : FcmTokenProvider {
                    override fun currentToken(): String? = null
                },
                registrar = object : FcmTokenRegistrar {
                    override fun register(token: String): Boolean = true
                }
            ),
            topicSubscriptionManager = FcmTopicSubscriptionManager(
                object : FcmSubscriptionClient {
                    override fun subscribe(topic: String): Boolean = true
                    override fun unsubscribe(topic: String): Boolean = true
                }
            ),
            stateStore = RuntimeStateStore()
        )
        val handler = PushTokenRefreshHandler(orchestrator)
        val state = handler.onNewToken("new-token")
        assertTrue(state.pushTokenSynced)
    }
}
