package com.rtw.pro.map.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapProviderConfigTest {
    @Test
    fun isReady_requiresApiKey() {
        assertFalse(MapProviderConfig(mapsApiKey = "").isReady())
        assertTrue(MapProviderConfig(mapsApiKey = "key").isReady())
        assertFalse(MapProviderConfig(mapsApiKey = "TODO_MAPS_API_KEY").isReady())
    }

    @Test
    fun streetViewConfig_validatesTimeoutRange() {
        assertTrue(StreetViewProviderConfig(timeoutMs = 6000L).isValid())
        assertFalse(StreetViewProviderConfig(timeoutMs = 200L).isValid())
    }
}
