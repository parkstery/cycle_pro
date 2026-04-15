package com.rtw.pro.foundation.data.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class GoogleSignInErrorMapperTest {
    @Test
    fun fromStatusCode_mapsKnownValues() {
        assertEquals(GoogleSignInFailureCode.CANCELLED, GoogleSignInErrorMapper.fromStatusCode(12501))
        assertEquals(GoogleSignInFailureCode.NETWORK_ERROR, GoogleSignInErrorMapper.fromStatusCode(7))
        assertEquals(GoogleSignInFailureCode.DEVELOPER_ERROR, GoogleSignInErrorMapper.fromStatusCode(10))
    }
}
