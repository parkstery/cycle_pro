package com.rtw.pro.map.data

data class MapProviderConfig(
    val mapsApiKey: String,
    val streetViewEnabled: Boolean = true
) {
    fun isReady(): Boolean {
        val normalized = mapsApiKey.trim()
        if (normalized.isBlank()) return false
        return !normalized.contains("TODO", ignoreCase = true)
    }
}

data class StreetViewProviderConfig(
    val timeoutMs: Long = 6000L,
    val fallbackToMapOnly: Boolean = true
) {
    fun isValid(): Boolean = timeoutMs in 1000L..15000L
}
