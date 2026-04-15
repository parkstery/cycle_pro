package com.rtw.pro.foundation.data.auth

/**
 * Android SDK bridge contracts.
 * Concrete classes should wrap Google Sign-In / Firebase Auth SDK.
 */
enum class GoogleSignInFailureCode {
    CANCELLED,
    NETWORK_ERROR,
    /** Misconfiguration: wrong Web client id, SHA-1, package name, etc. (often Play Services status code 10) */
    DEVELOPER_ERROR,
    INVALID_ACCOUNT,
    UNKNOWN
}

data class GoogleSignInRequestResult(
    val idToken: String? = null,
    val failureCode: GoogleSignInFailureCode? = null
) {
    val success: Boolean get() = !idToken.isNullOrBlank()
}

interface AndroidGoogleSignInBridge {
    fun requestIdTokenInteractive(): String?
    fun requestIdTokenInteractiveResult(): GoogleSignInRequestResult {
        val token = requestIdTokenInteractive()
        return if (token.isNullOrBlank()) {
            GoogleSignInRequestResult(idToken = null, failureCode = GoogleSignInFailureCode.CANCELLED)
        } else {
            GoogleSignInRequestResult(idToken = token)
        }
    }
    fun signOut()
}

interface AndroidFirebaseAuthBridge {
    fun currentUid(): String?
    fun currentAccessToken(): String?
    fun signInWithGoogleIdToken(idToken: String): Boolean
    fun signInWithGoogleIdTokenResult(idToken: String): FirebaseSignInResult {
        val ok = signInWithGoogleIdToken(idToken)
        return FirebaseSignInResult(
            success = ok,
            errorCode = if (ok) null else FirebaseSignInErrorCode.UNKNOWN
        )
    }
    fun signOut()
}

class AndroidGoogleSignInClient(
    private val bridge: AndroidGoogleSignInBridge
) : GoogleSignInClient {
    override fun requestIdTokenDetailed(): GoogleSignInRequestResult {
        return bridge.requestIdTokenInteractiveResult()
    }

    override fun requestIdToken(): String? {
        return requestIdTokenDetailed().idToken
    }

    override fun signOut() {
        bridge.signOut()
    }
}

class AndroidFirebaseAuthClient(
    private val bridge: AndroidFirebaseAuthBridge
) : FirebaseAuthClient {
    override fun currentUserUid(): String? = bridge.currentUid()
    override fun currentAccessToken(): String? = bridge.currentAccessToken()
    override fun signInWithGoogleIdToken(idToken: String): Boolean = bridge.signInWithGoogleIdToken(idToken)
    override fun signInWithGoogleIdTokenDetailed(idToken: String): FirebaseSignInResult {
        return bridge.signInWithGoogleIdTokenResult(idToken)
    }

    override fun signOut() {
        bridge.signOut()
    }
}
