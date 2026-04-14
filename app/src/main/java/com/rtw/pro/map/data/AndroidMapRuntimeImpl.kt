package com.rtw.pro.map.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.maps.MapsInitializer

/**
 * Android map/streetview runtime skeleton.
 */
class MapPermissionGatewayImpl(
    private val context: Context
) : MapPermissionGateway {
    override fun locationPermissionState(): MapPermissionState {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return if (fine || coarse) MapPermissionState.GRANTED else MapPermissionState.DENIED
    }
}

class GoogleMapSdkGatewayImpl(
    private val context: Context
) : GoogleMapSdkGateway {
    override fun initialize(apiKey: String): Boolean {
        if (apiKey.isBlank()) return false
        val status = MapsInitializer.initialize(context)
        return status == ConnectionResult.SUCCESS
    }
}

class StreetViewSdkGatewayImpl(
    private val context: Context
) : StreetViewSdkGateway {
    override fun initialize(timeoutMs: Long): Boolean {
        if (timeoutMs !in 1000L..15000L) return false
        val status = MapsInitializer.initialize(context)
        return status == ConnectionResult.SUCCESS
    }
}
