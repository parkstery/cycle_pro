package com.rtw.pro

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import com.liveonsoft.cyclepro.BuildConfig
import com.rtw.pro.app.AppRuntimeComposition
import com.rtw.pro.app.runtime.RuntimeState

class MainActivity : AppCompatActivity() {
    private lateinit var dashboardText: TextView
    private val runtimeOrchestrator by lazy { AppRuntimeComposition.provideAppRuntimeOrchestrator(applicationContext) }
    private var currentState: RuntimeState = RuntimeState()
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        AppRuntimeComposition.dispatchGoogleSignInActivityResult(result.data)
        renderRuntimeState()
    }
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        renderRuntimeState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dashboardText = TextView(this).apply {
            textSize = 16f
            setPadding(48, 32, 48, 32)
        }
        val refreshButton = Button(this).apply {
            text = "Refresh Runtime State"
            setOnClickListener { renderRuntimeState() }
        }
        val retryPushTokenButton = Button(this).apply {
            text = "Retry Push Token Sync"
            setOnClickListener {
                currentState = runtimeOrchestrator.retryPushTokenSync()
                renderCurrentState()
            }
        }
        val retryAuthButton = Button(this).apply {
            text = "Retry Auth Sign-In"
            setOnClickListener {
                currentState = runtimeOrchestrator.retryAuthSignIn()
                renderCurrentState()
            }
        }
        val signOutButton = Button(this).apply {
            text = "Sign Out Auth"
            setOnClickListener {
                currentState = runtimeOrchestrator.signOutAuth()
                renderCurrentState()
            }
        }
        val googleSignInButton = Button(this).apply {
            text = "Google Sign-In (Interactive)"
            setOnClickListener {
                val intent = AppRuntimeComposition.launchGoogleSignInIntent(this@MainActivity)
                if (intent != null) {
                    googleSignInLauncher.launch(intent)
                } else {
                    renderCurrentState()
                }
            }
        }
        val subscribeTopicButton = Button(this).apply {
            text = "Subscribe Topic: daily-20h"
            setOnClickListener {
                currentState = runtimeOrchestrator.subscribeEventTopic("daily-20h")
                renderCurrentState()
            }
        }
        val requestLocationPermissionButton = Button(this).apply {
            text = "Request Location Permission"
            setOnClickListener {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 32)
            addView(refreshButton)
            addView(retryPushTokenButton)
            addView(retryAuthButton)
            addView(signOutButton)
            addView(googleSignInButton)
            addView(subscribeTopicButton)
            addView(requestLocationPermissionButton)
            addView(dashboardText)
        }
        setContentView(ScrollView(this).apply { addView(layout) })

        renderRuntimeState()
    }

    private fun renderRuntimeState() {
        currentState = runtimeOrchestrator.onAppLaunch(
            mapConfig = AppRuntimeComposition.defaultMapConfig(),
            streetViewConfig = AppRuntimeComposition.defaultStreetViewConfig()
        )
        renderCurrentState()
    }

    private fun renderCurrentState() {
        val authConfigLooksReal = !BuildConfig.AUTH_WEB_CLIENT_ID.contains("TODO", ignoreCase = true) &&
            !BuildConfig.AUTH_FIREBASE_PROJECT_ID.contains("TODO", ignoreCase = true)
        val integrationReadyForAuth = authConfigLooksReal && BuildConfig.HAS_GOOGLE_SERVICES_JSON
        val mapApiKeyConfigured = !BuildConfig.MAPS_API_KEY.contains("TODO", ignoreCase = true) &&
            BuildConfig.MAPS_API_KEY.isNotBlank()
        val blockers = buildList {
            if (!BuildConfig.HAS_GOOGLE_SERVICES_JSON) add("missing google-services.json")
            if (!authConfigLooksReal) add("auth config placeholder")
            if (!mapApiKeyConfigured) add("map api key placeholder")
            if (currentState.mapErrorCode?.name == "LOCATION_PERMISSION_DENIED") {
                add("location permission denied")
            }
        }
        val integrationGatePassed = blockers.isEmpty()
        val webClientIdHint = BuildConfig.AUTH_WEB_CLIENT_ID.let { value ->
            if (value.length <= 10) value else "${value.take(6)}...${value.takeLast(4)}"
        }
        val nextAction = when {
            currentState.mapErrorCode?.name == "LOCATION_PERMISSION_DENIED" ->
                "Tap Request Location Permission and allow access."
            !BuildConfig.HAS_GOOGLE_SERVICES_JSON -> "Place app/google-services.json from Firebase console."
            !authConfigLooksReal -> "Set rtw.auth.webClientId and rtw.auth.firebaseProjectId in local.properties."
            else -> "Run Google Sign-In button and verify authReady=true."
        }
        val mapNextAction = when (currentState.mapErrorCode?.name) {
            "LOCATION_PERMISSION_DENIED" -> "Allow location permission and refresh runtime state."
            "MAP_API_KEY_MISSING" -> "Set a valid map API key in runtime config."
            "MAP_SDK_INIT_FAILED" -> "Verify Google Play services and map API key setup."
            else -> "Map runtime status is stable."
        }
        dashboardText.text = buildString {
            appendLine("Ride The World Pro")
            appendLine("Runtime status dashboard")
            appendLine("appVersion: ${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})")
            appendLine("buildGitSha: ${BuildConfig.BUILD_GIT_SHA}")
            appendLine()
            appendLine("authIntegrationReady: $integrationReadyForAuth")
            appendLine("authConfigLooksReal: $authConfigLooksReal")
            appendLine("authWebClientIdHint: $webClientIdHint")
            appendLine("authFirebaseProjectId: ${BuildConfig.AUTH_FIREBASE_PROJECT_ID}")
            appendLine("googleServicesJsonPresent: ${BuildConfig.HAS_GOOGLE_SERVICES_JSON}")
            appendLine("mapApiKeyConfigured: $mapApiKeyConfigured")
            appendLine("integrationGatePassed: $integrationGatePassed")
            appendLine("authReady: ${currentState.authReady}")
            appendLine("authStatus: ${currentState.authStatus}")
            appendLine("authMessage: ${currentState.authMessage.ifBlank { "(empty)" }}")
            appendLine("mapReady: ${currentState.mapReady}")
            appendLine("mapMode: ${currentState.mapMode}")
            appendLine("mapErrorCode: ${currentState.mapErrorCode ?: "none"}")
            appendLine("pushTokenSynced: ${currentState.pushTokenSynced}")
            appendLine("pushTokenErrorCode: ${currentState.pushTokenErrorCode ?: "none"}")
            appendLine("pushTokenMessage: ${currentState.pushTokenMessage.ifBlank { "(empty)" }}")
            appendLine("pushTopicSubscribed: ${currentState.pushTopicSubscribed}")
            appendLine("pushTopic: ${currentState.pushTopic.ifBlank { "(empty)" }}")
            appendLine("pushTopicErrorCode: ${currentState.pushTopicErrorCode ?: "none"}")
            appendLine("pushTopicMessage: ${currentState.pushTopicMessage.ifBlank { "(empty)" }}")
            appendLine()
            appendLine("mapMessage:")
            appendLine(currentState.mapMessage.ifBlank { "(empty)" })
            appendLine()
            appendLine("mapNextAction:")
            appendLine(mapNextAction)
            appendLine()
            appendLine("integrationBlockers:")
            if (blockers.isEmpty()) {
                appendLine("(none)")
            } else {
                appendLine(blockers.joinToString(separator = ", "))
            }
            appendLine()
            appendLine("nextAction:")
            appendLine(nextAction)
            appendLine()
            appendLine("refreshTimeMs: ${System.currentTimeMillis()}")
        }
    }
}
