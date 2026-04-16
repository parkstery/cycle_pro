package com.rtw.pro.map.domain

import com.rtw.pro.baseline.ui.streetview.StreetViewMode
import com.rtw.pro.map.data.AndroidMapRuntimeBinder
import com.rtw.pro.map.data.GoogleMapSdkGateway
import com.rtw.pro.map.data.MapBindErrorCode
import com.rtw.pro.map.data.MapPermissionGateway
import com.rtw.pro.map.data.MapPermissionState
import com.rtw.pro.map.data.MapProviderConfig
import com.rtw.pro.map.data.StreetViewProviderConfig
import com.rtw.pro.map.data.StreetViewSdkGateway
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapRuntimeOrchestratorTest {
    @Test
    fun prepare_returnsStreetViewMode_whenBindSuccess() {
        val orchestrator = MapRuntimeOrchestrator(
            binder = AndroidMapRuntimeBinder(
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
        )
        val state = orchestrator.prepare(
            mapConfig = MapProviderConfig("map-key", streetViewEnabled = true),
            streetViewConfig = StreetViewProviderConfig(6000L, true)
        )
        assertTrue(state.ready)
        assertEquals(StreetViewMode.STREETVIEW, state.streetViewMode)
    }

    @Test
    fun prepare_returnsMapOnly_whenPermissionDenied() {
        val orchestrator = MapRuntimeOrchestrator(
            binder = AndroidMapRuntimeBinder(
                permissionGateway = object : MapPermissionGateway {
                    override fun locationPermissionState(): MapPermissionState = MapPermissionState.DENIED
                },
                mapGateway = object : GoogleMapSdkGateway {
                    override fun initialize(apiKey: String): Boolean = true
                },
                streetViewGateway = object : StreetViewSdkGateway {
                    override fun initialize(timeoutMs: Long): Boolean = true
                }
            )
        )
        val state = orchestrator.prepare(
            mapConfig = MapProviderConfig("map-key", streetViewEnabled = true),
            streetViewConfig = StreetViewProviderConfig(6000L, true)
        )
        assertEquals(false, state.ready)
        assertEquals(StreetViewMode.MAP_ONLY, state.streetViewMode)
        assertEquals(MapBindErrorCode.LOCATION_PERMISSION_DENIED, state.errorCode)
    }

    @Test
    fun prepare_returnsReadyMapOnlyWithFallbackMessage_whenStreetViewInitFailsButFallbackEnabled() {
        val orchestrator = MapRuntimeOrchestrator(
            binder = AndroidMapRuntimeBinder(
                permissionGateway = object : MapPermissionGateway {
                    override fun locationPermissionState(): MapPermissionState = MapPermissionState.GRANTED
                },
                mapGateway = object : GoogleMapSdkGateway {
                    override fun initialize(apiKey: String): Boolean = true
                },
                streetViewGateway = object : StreetViewSdkGateway {
                    override fun initialize(timeoutMs: Long): Boolean = false
                }
            )
        )
        val state = orchestrator.prepare(
            mapConfig = MapProviderConfig("map-key", streetViewEnabled = true),
            streetViewConfig = StreetViewProviderConfig(6000L, fallbackToMapOnly = true)
        )
        assertTrue(state.ready)
        assertEquals(StreetViewMode.MAP_ONLY, state.streetViewMode)
        assertEquals(MapBindErrorCode.STREETVIEW_SDK_INIT_FAILED, state.errorCode)
        assertEquals("거리뷰 초기화에 실패해 지도 모드로 전환합니다.", state.message)
    }
}
