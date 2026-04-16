package com.rtw.pro.map.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidMapRuntimeBinderTest {
    @Test
    fun bind_returnsReady_whenPermissionAndSdkInitSucceed() {
        val binder = AndroidMapRuntimeBinder(
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
        val result = binder.bind(
            mapConfig = MapProviderConfig(mapsApiKey = "key", streetViewEnabled = true),
            svConfig = StreetViewProviderConfig(timeoutMs = 6000L)
        )
        assertTrue(result.ready)
        assertEquals("ready", result.reason)
    }

    @Test
    fun bind_returnsReadyMapOnlyFallback_whenStreetViewInitFails_andFallbackEnabled() {
        val binder = AndroidMapRuntimeBinder(
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
        val result = binder.bind(
            mapConfig = MapProviderConfig(mapsApiKey = "key", streetViewEnabled = true),
            svConfig = StreetViewProviderConfig(timeoutMs = 6000L, fallbackToMapOnly = true)
        )
        assertTrue(result.ready)
        assertEquals(AndroidMapRuntimeBinder.REASON_STREETVIEW_FALLBACK_MAP_ONLY, result.reason)
    }

    @Test
    fun bind_returnsFailure_whenStreetViewInitFails_andFallbackDisabled() {
        val binder = AndroidMapRuntimeBinder(
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
        val result = binder.bind(
            mapConfig = MapProviderConfig(mapsApiKey = "key", streetViewEnabled = true),
            svConfig = StreetViewProviderConfig(timeoutMs = 6000L, fallbackToMapOnly = false)
        )
        assertEquals(false, result.ready)
        assertEquals("streetview-sdk-init-failed", result.reason)
    }

    @Test
    fun bind_returnsStreetViewConfigInvalid_whenTimeoutOutOfRange() {
        val binder = AndroidMapRuntimeBinder(
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
        val result = binder.bind(
            mapConfig = MapProviderConfig(mapsApiKey = "key", streetViewEnabled = true),
            svConfig = StreetViewProviderConfig(timeoutMs = 500L, fallbackToMapOnly = true)
        )
        assertEquals(false, result.ready)
        assertEquals("streetview-config-invalid", result.reason)
    }
}
