package com.rtw.pro.app

import android.content.Context
import android.content.Intent
import com.liveonsoft.cyclepro.BuildConfig
import com.rtw.pro.app.runtime.AppRuntimeOrchestrator
import com.rtw.pro.app.runtime.MainAppStartupHandler
import com.rtw.pro.app.runtime.PushTokenRefreshHandler
import com.rtw.pro.app.runtime.RuntimeStateStore
import com.rtw.pro.foundation.data.auth.AndroidFirebaseAuthBridgeImpl
import com.rtw.pro.foundation.data.auth.AndroidFirebaseAuthClient
import com.rtw.pro.foundation.data.auth.AndroidGoogleSignInBridgeImpl
import com.rtw.pro.foundation.data.auth.AndroidGoogleSignInClient
import com.rtw.pro.foundation.data.auth.AuthProviderConfig
import com.rtw.pro.foundation.data.auth.AuthRuntimeCoordinator
import com.rtw.pro.foundation.data.auth.FirebaseAuthGateway
import com.rtw.pro.map.data.AndroidMapRuntimeBinder
import com.rtw.pro.map.data.GoogleMapSdkGatewayImpl
import com.rtw.pro.map.data.MapProviderConfig
import com.rtw.pro.map.data.MapPermissionGatewayImpl
import com.rtw.pro.map.data.StreetViewSdkGatewayImpl
import com.rtw.pro.map.data.StreetViewProviderConfig
import com.rtw.pro.map.domain.MapRuntimeOrchestrator
import com.rtw.pro.notification.data.FcmPushNotifier
import com.rtw.pro.notification.data.HttpTopicPushFcmClient
import com.rtw.pro.notification.data.FcmSubscriptionClientImpl
import com.rtw.pro.notification.data.FcmTopicSubscriptionManager
import com.rtw.pro.notification.data.FcmTokenProviderImpl
import com.rtw.pro.notification.data.FcmTokenRegistrarImpl
import com.rtw.pro.notification.data.FcmTokenSyncCoordinator

/**
 * App-level composition skeleton.
 * Replace TODO clients with real Android SDK implementations.
 */
object AppRuntimeComposition {
    private val googleSignInBridge by lazy { AndroidGoogleSignInBridgeImpl() }
    private val firebaseAuthBridge by lazy { AndroidFirebaseAuthBridgeImpl() }
    private fun authConfig(): AuthProviderConfig = AuthProviderConfig(
        googleWebClientId = BuildConfig.AUTH_WEB_CLIENT_ID,
        firebaseProjectId = BuildConfig.AUTH_FIREBASE_PROJECT_ID
    )

    fun provideAuthGateway(): FirebaseAuthGateway {
        val googleClient = AndroidGoogleSignInClient(googleSignInBridge)
        val firebaseClient = AndroidFirebaseAuthClient(firebaseAuthBridge)
        return FirebaseAuthGateway(firebaseClient, googleClient)
    }

    fun dispatchGoogleSignInResultToken(idToken: String?) {
        googleSignInBridge.onSignInResultToken(idToken)
    }

    fun dispatchGoogleSignInActivityResult(data: Intent?) {
        googleSignInBridge.onSignInResultFromIntent(data)
    }

    fun dispatchGoogleSignInResultErrorStatus(statusCode: Int?) {
        googleSignInBridge.onSignInResultErrorStatus(statusCode)
    }

    fun launchGoogleSignInIntent(context: Context): Intent? {
        return googleSignInBridge.launchSignInIntent(
            context = context,
            webClientId = authConfig().googleWebClientId
        )
    }

    fun provideMapBinder(context: Context): AndroidMapRuntimeBinder {
        return AndroidMapRuntimeBinder(
            permissionGateway = MapPermissionGatewayImpl(context),
            mapGateway = GoogleMapSdkGatewayImpl(context),
            streetViewGateway = StreetViewSdkGatewayImpl(context)
        )
    }

    fun provideMapRuntimeOrchestrator(context: Context): MapRuntimeOrchestrator {
        return MapRuntimeOrchestrator(provideMapBinder(context))
    }

    fun provideTokenSyncCoordinator(): FcmTokenSyncCoordinator {
        return FcmTokenSyncCoordinator(
            provider = FcmTokenProviderImpl(),
            registrar = FcmTokenRegistrarImpl()
        )
    }

    fun provideTopicSubscriptionManager(): FcmTopicSubscriptionManager {
        return FcmTopicSubscriptionManager(FcmSubscriptionClientImpl())
    }

    fun providePushNotifier(): FcmPushNotifier {
        val url = BuildConfig.PUSH_TOPIC_SEND_URL.trim()
        val client = if (url.isBlank() || url.contains("TODO", ignoreCase = true)) {
            object : com.rtw.pro.notification.data.FcmClient {
                override fun sendToTopic(topic: String, title: String, body: String): Boolean = false
            }
        } else {
            HttpTopicPushFcmClient(
                endpointUrl = url,
                idTokenProvider = {
                    provideAuthGateway().getCurrentSession()?.accessToken
                }
            )
        }
        return FcmPushNotifier(client)
    }

    fun provideAppRuntimeOrchestrator(context: Context): AppRuntimeOrchestrator {
        val authGateway = provideAuthGateway()
        val authCoordinator = AuthRuntimeCoordinator(
            config = authConfig(),
            authGateway = authGateway
        )
        val mapBinder = provideMapBinder(context)
        val mapRuntime = MapRuntimeOrchestrator(mapBinder)
        val tokenSync = provideTokenSyncCoordinator()
        val topicManager = provideTopicSubscriptionManager()
        return AppRuntimeOrchestrator(
            authCoordinator = authCoordinator,
            mapRuntimeOrchestrator = mapRuntime,
            tokenSyncCoordinator = tokenSync,
            topicSubscriptionManager = topicManager,
            stateStore = RuntimeStateStore()
        )
    }

    fun defaultMapConfig(): MapProviderConfig = MapProviderConfig(
        mapsApiKey = BuildConfig.MAPS_API_KEY,
        streetViewEnabled = true
    )

    fun defaultStreetViewConfig(): StreetViewProviderConfig = StreetViewProviderConfig(
        timeoutMs = 6000L,
        fallbackToMapOnly = true
    )

    fun provideMainAppStartupHandler(context: Context): MainAppStartupHandler {
        return MainAppStartupHandler(provideAppRuntimeOrchestrator(context))
    }

    fun providePushTokenRefreshHandler(context: Context): PushTokenRefreshHandler {
        return PushTokenRefreshHandler(provideAppRuntimeOrchestrator(context))
    }
}
