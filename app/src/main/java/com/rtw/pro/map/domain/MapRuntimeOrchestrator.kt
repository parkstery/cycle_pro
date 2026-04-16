package com.rtw.pro.map.domain

import com.rtw.pro.baseline.ui.streetview.StreetViewMode
import com.rtw.pro.map.data.AndroidMapRuntimeBinder
import com.rtw.pro.map.data.MapBindErrorMapper
import com.rtw.pro.map.data.MapBindErrorCode
import com.rtw.pro.map.data.MapProviderConfig
import com.rtw.pro.map.data.StreetViewProviderConfig

data class MapRuntimeUiState(
    val ready: Boolean,
    val streetViewMode: StreetViewMode,
    val message: String,
    val errorCode: com.rtw.pro.map.data.MapBindErrorCode? = null
)

class MapRuntimeOrchestrator(
    private val binder: AndroidMapRuntimeBinder
) {
    fun prepare(
        mapConfig: MapProviderConfig,
        streetViewConfig: StreetViewProviderConfig
    ): MapRuntimeUiState {
        val bind = binder.bind(mapConfig, streetViewConfig)
        if (bind.ready) {
            val isFallbackToMapOnly = bind.reason == AndroidMapRuntimeBinder.REASON_STREETVIEW_FALLBACK_MAP_ONLY
            return MapRuntimeUiState(
                ready = true,
                streetViewMode = if (mapConfig.streetViewEnabled && !isFallbackToMapOnly) {
                    StreetViewMode.STREETVIEW
                } else {
                    StreetViewMode.MAP_ONLY
                },
                message = if (isFallbackToMapOnly) {
                    MapUiMessagePolicy.bindMessage(MapBindErrorCode.STREETVIEW_SDK_INIT_FAILED)
                } else {
                    MapUiMessagePolicy.bindMessage(null)
                },
                errorCode = if (isFallbackToMapOnly) MapBindErrorCode.STREETVIEW_SDK_INIT_FAILED else null
            )
        }

        val code = MapBindErrorMapper.fromReason(bind.reason)
        return MapRuntimeUiState(
            ready = false,
            streetViewMode = StreetViewMode.MAP_ONLY,
            message = MapUiMessagePolicy.bindMessage(code),
            errorCode = code
        )
    }
}
