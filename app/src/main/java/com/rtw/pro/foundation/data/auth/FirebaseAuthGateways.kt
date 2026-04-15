package com.rtw.pro.foundation.data.auth

import com.rtw.pro.foundation.domain.auth.AuthError
import com.rtw.pro.foundation.domain.auth.AuthGateway
import com.rtw.pro.foundation.domain.auth.AuthResult
import com.rtw.pro.foundation.domain.auth.AuthSession

enum class FirebaseSignInErrorCode {
    APP_NOT_CONFIGURED,
    INVALID_GOOGLE_TOKEN,
    NETWORK_ERROR,
    USER_DISABLED,
    TOKEN_EXPIRED,
    UNKNOWN
}

data class FirebaseSignInResult(
    val success: Boolean,
    val errorCode: FirebaseSignInErrorCode? = null,
    val rawErrorCode: String? = null,
    val rawErrorDetail: String? = null
)

interface FirebaseAuthClient {
    fun currentUserUid(): String?
    fun currentAccessToken(): String?
    fun signInWithGoogleIdToken(idToken: String): Boolean
    fun signInWithGoogleIdTokenDetailed(idToken: String): FirebaseSignInResult {
        val ok = signInWithGoogleIdToken(idToken)
        return FirebaseSignInResult(
            success = ok,
            errorCode = if (ok) null else FirebaseSignInErrorCode.UNKNOWN
        )
    }
    fun signOut() {}
}

interface GoogleSignInClient {
    fun requestIdToken(): String?

    /**
     * Full Google Sign-In result for interactive flows.
     * Default derives from [requestIdToken] for tests/simple fakes.
     */
    fun requestIdTokenDetailed(): GoogleSignInRequestResult {
        val token = requestIdToken()
        return if (token.isNullOrBlank()) {
            GoogleSignInRequestResult(idToken = null, failureCode = GoogleSignInFailureCode.UNKNOWN)
        } else {
            GoogleSignInRequestResult(idToken = token)
        }
    }

    fun signOut() {}
}

class FirebaseAuthGateway(
    private val firebaseAuthClient: FirebaseAuthClient,
    private val googleSignInClient: GoogleSignInClient
) : AuthGateway {
    override fun getCurrentSession(): AuthSession? {
        val uid = firebaseAuthClient.currentUserUid() ?: return null
        val token = firebaseAuthClient.currentAccessToken() ?: return null
        return AuthSession(uid = uid, accessToken = token)
    }

    override fun signInWithGoogle(): AuthResult {
        val google = googleSignInClient.requestIdTokenDetailed()
        if (!google.success) {
            return when (google.failureCode) {
                GoogleSignInFailureCode.CANCELLED -> AuthResult.Failure(AuthError.Cancelled)
                GoogleSignInFailureCode.NETWORK_ERROR -> AuthResult.Failure(AuthError.Unknown("network-error"))
                GoogleSignInFailureCode.INVALID_ACCOUNT ->
                    AuthResult.Failure(AuthError.Unknown("google-invalid-account"))
                GoogleSignInFailureCode.UNKNOWN, null ->
                    AuthResult.Failure(AuthError.Unknown("google-no-id-token"))
            }
        }
        val idToken = google.idToken ?: return AuthResult.Failure(AuthError.Unknown("google-no-id-token"))
        val signed = firebaseAuthClient.signInWithGoogleIdTokenDetailed(idToken)
        if (!signed.success) {
            return when (signed.errorCode) {
                FirebaseSignInErrorCode.APP_NOT_CONFIGURED -> {
                    AuthResult.Failure(AuthError.Unknown("app-not-configured"))
                }
                FirebaseSignInErrorCode.TOKEN_EXPIRED -> AuthResult.Failure(AuthError.TokenExpired)
                FirebaseSignInErrorCode.NETWORK_ERROR -> AuthResult.Failure(AuthError.Unknown("network-error"))
                FirebaseSignInErrorCode.INVALID_GOOGLE_TOKEN -> AuthResult.Failure(AuthError.Unknown("invalid-google-token"))
                FirebaseSignInErrorCode.USER_DISABLED -> AuthResult.Failure(AuthError.Unknown("user-disabled"))
                FirebaseSignInErrorCode.UNKNOWN, null -> {
                    val raw = signed.rawErrorCode ?: "unknown"
                    val detail = signed.rawErrorDetail?.replace(":", "=")?.replace("\n", " ") ?: "none"
                    AuthResult.Failure(AuthError.Unknown("firebase-sign-in-failed:$raw:$detail"))
                }
            }
        }
        return getCurrentSession()?.let { AuthResult.Success(it) }
            ?: AuthResult.Failure(AuthError.TokenExpired)
    }

    fun signOutAll() {
        firebaseAuthClient.signOut()
        googleSignInClient.signOut()
    }

    override fun signOut() {
        signOutAll()
    }
}
