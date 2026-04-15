package com.rtw.pro.foundation.data.auth

object GoogleSignInErrorMapper {
    /**
     * Maps common Google sign-in status codes to domain failure code.
     * Unknown codes are treated as UNKNOWN.
     */
    fun fromStatusCode(statusCode: Int?): GoogleSignInFailureCode {
        return when (statusCode) {
            12501 -> GoogleSignInFailureCode.CANCELLED
            7 -> GoogleSignInFailureCode.NETWORK_ERROR
            // Google Sign-In: DEVELOPER_ERROR — Web client ID, SHA-1 fingerprints, or package name mismatch.
            10 -> GoogleSignInFailureCode.DEVELOPER_ERROR
            else -> GoogleSignInFailureCode.UNKNOWN
        }
    }
}
