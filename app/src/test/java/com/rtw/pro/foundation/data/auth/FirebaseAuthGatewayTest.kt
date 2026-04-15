package com.rtw.pro.foundation.data.auth

import com.rtw.pro.foundation.domain.auth.AuthError
import com.rtw.pro.foundation.domain.auth.AuthResult
import kotlin.test.Test
import kotlin.test.assertTrue

class FirebaseAuthGatewayTest {
    @Test
    fun signInWithGoogle_returnsSuccess_whenClientsSucceed() {
        val gateway = FirebaseAuthGateway(
            firebaseAuthClient = object : FirebaseAuthClient {
                override fun currentUserUid(): String? = "uid-1"
                override fun currentAccessToken(): String? = "token-1"
                override fun signInWithGoogleIdToken(idToken: String): Boolean = true
            },
            googleSignInClient = object : GoogleSignInClient {
                override fun requestIdToken(): String? = "google-id-token"
            }
        )
        val result = gateway.signInWithGoogle()
        assertTrue(result is AuthResult.Success)
    }

    @Test
    fun signInWithGoogle_returnsCancelled_onlyWhenGoogleReportsCancelled() {
        val gateway = FirebaseAuthGateway(
            firebaseAuthClient = object : FirebaseAuthClient {
                override fun currentUserUid(): String? = null
                override fun currentAccessToken(): String? = null
                override fun signInWithGoogleIdToken(idToken: String): Boolean = false
            },
            googleSignInClient = object : GoogleSignInClient {
                override fun requestIdTokenDetailed(): GoogleSignInRequestResult {
                    return GoogleSignInRequestResult(
                        idToken = null,
                        failureCode = GoogleSignInFailureCode.CANCELLED
                    )
                }

                override fun requestIdToken(): String? = null
            }
        )
        val result = gateway.signInWithGoogle()
        assertTrue(result is AuthResult.Failure)
        assertTrue((result as AuthResult.Failure).error is AuthError.Cancelled)
    }

    @Test
    fun signInWithGoogle_returnsNoTokenFailure_whenGoogleReturnsUnknownWithoutToken() {
        val gateway = FirebaseAuthGateway(
            firebaseAuthClient = object : FirebaseAuthClient {
                override fun currentUserUid(): String? = null
                override fun currentAccessToken(): String? = null
                override fun signInWithGoogleIdToken(idToken: String): Boolean = false
            },
            googleSignInClient = object : GoogleSignInClient {
                override fun requestIdTokenDetailed(): GoogleSignInRequestResult {
                    return GoogleSignInRequestResult(
                        idToken = null,
                        failureCode = GoogleSignInFailureCode.UNKNOWN
                    )
                }

                override fun requestIdToken(): String? = null
            }
        )
        val result = gateway.signInWithGoogle()
        assertTrue(result is AuthResult.Failure)
        val err = (result as AuthResult.Failure).error
        assertTrue(err is AuthError.Unknown && (err as AuthError.Unknown).message == "google-no-id-token")
    }
}
