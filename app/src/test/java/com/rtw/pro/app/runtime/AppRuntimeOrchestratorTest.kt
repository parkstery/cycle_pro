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
import com.rtw.pro.map.data.MapProviderConfig
import com.rtw.pro.map.data.StreetViewProviderConfig
import com.rtw.pro.map.data.StreetViewSdkGateway
import com.rtw.pro.map.domain.MapRuntimeOrchestrator
import com.rtw.pro.notification.data.FcmTokenProvider
import com.rtw.pro.notification.data.FcmTokenRegistrar
import com.rtw.pro.notification.data.FcmTokenSyncCoordinator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppRuntimeOrchestratorTest {
    @Test
    fun onAppLaunch_updatesAllReadyFlags_whenDependenciesSucceed() {
        val authCoordinator = AuthRuntimeCoordinator(
            config = AuthProviderConfig("web", "project"),
            authGateway = object : AuthGateway {
                override fun getCurrentSession(): AuthSession? = AuthSession("u1", "t1")
                override fun signInWithGoogle(): AuthResult = AuthResult.Success(AuthSession("u2", "t2"))
            }
        )
        val mapRuntime = MapRuntimeOrchestrator(AndroidMapRuntimeBinder(
            permissionGateway = object : MapPermissionGateway {
                override fun locationPermissionState(): MapPermissionState = MapPermissionState.GRANTED
            },
            mapGateway = object : GoogleMapSdkGateway {
                override fun initialize(apiKey: String): Boolean = true
            },
            streetViewGateway = object : StreetViewSdkGateway {
                override fun initialize(timeoutMs: Long): Boolean = true
            }
        ))
        val tokenSync = FcmTokenSyncCoordinator(
            provider = object : FcmTokenProvider {
                override fun currentToken(): String? = "token-1"
            },
            registrar = object : FcmTokenRegistrar {
                override fun register(token: String): Boolean = true
            }
        )
        val orchestrator = AppRuntimeOrchestrator(
            authCoordinator = authCoordinator,
            mapRuntimeOrchestrator = mapRuntime,
            tokenSyncCoordinator = tokenSync,
            stateStore = RuntimeStateStore()
        )

        val state = orchestrator.onAppLaunch(
            mapConfig = MapProviderConfig("map-key", true),
            streetViewConfig = StreetViewProviderConfig(6000L, true)
        )
        assertTrue(state.authReady)
        assertTrue(state.mapReady)
        assertTrue(state.pushTokenSynced)
        assertEquals("지도가 정상 준비되었습니다.", state.mapMessage)
    }
}
