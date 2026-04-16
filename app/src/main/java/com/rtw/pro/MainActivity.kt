package com.rtw.pro

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import com.liveonsoft.cyclepro.BuildConfig
import com.rtw.pro.app.AppRuntimeComposition
import com.rtw.pro.app.runtime.RuntimeState
import com.rtw.pro.notification.domain.PushMessage
import com.rtw.pro.notification.data.LocalNotificationSender
import androidx.core.content.ContextCompat
import com.rtw.pro.roomrace.domain.RoomRaceState
import com.rtw.pro.roomrace.domain.RoomRaceStateMachine

class MainActivity : AppCompatActivity() {
    private lateinit var dashboardText: TextView
    private val runtimeOrchestrator by lazy { AppRuntimeComposition.provideAppRuntimeOrchestrator(applicationContext) }
    private var currentState: RuntimeState = RuntimeState()
    private var hasLaunchedRuntime: Boolean = false
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        AppRuntimeComposition.dispatchGoogleSignInActivityResult(result.data)
        retryAuthSignInAsync()
    }
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshMapAndPushState()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        renderCurrentState()
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
                retryPushTokenSyncAsync()
            }
        }
        val retryAuthButton = Button(this).apply {
            text = "Retry Auth Sign-In"
            setOnClickListener {
                retryAuthSignInAsync()
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
                subscribeTopicAsync("daily-20h")
            }
        }

        val simulateRaceEndButton = Button(this).apply {
            text = "Simulate Race End -> Notify"
            setOnClickListener {
                simulateRaceEndAsync()
            }
        }

        val sendPushSmokeTestButton = Button(this).apply {
            text = "Send Test Push (fallback): daily-20h"
            setOnClickListener {
                sendPushSmokeTestAsync(
                    topic = "daily-20h",
                    title = "RTW Pro",
                    body = "Test push (fallback smoke test)"
                )
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
            addView(simulateRaceEndButton)
            addView(sendPushSmokeTestButton)
            addView(requestLocationPermissionButton)
            addView(dashboardText)
        }
        setContentView(ScrollView(this).apply { addView(layout) })

        maybeRequestNotificationPermission()
        renderRuntimeState()
    }

    private fun maybeRequestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT < 33) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun renderRuntimeState() {
        val mapConfig = AppRuntimeComposition.defaultMapConfig()
        val streetViewConfig = AppRuntimeComposition.defaultStreetViewConfig()
        currentState = if (!hasLaunchedRuntime) {
            hasLaunchedRuntime = true
            runtimeOrchestrator.onAppLaunch(
                mapConfig = mapConfig,
                streetViewConfig = streetViewConfig
            )
        } else {
            runtimeOrchestrator.refreshMapAndPush(
                mapConfig = mapConfig,
                streetViewConfig = streetViewConfig
            )
        }
        renderCurrentState()
    }

    private fun refreshMapAndPushState() {
        currentState = runtimeOrchestrator.refreshMapAndPush(
            mapConfig = AppRuntimeComposition.defaultMapConfig(),
            streetViewConfig = AppRuntimeComposition.defaultStreetViewConfig()
        )
        renderCurrentState()
    }

    private fun retryAuthSignInAsync() {
        Thread {
            val nextState = runtimeOrchestrator.retryAuthSignIn()
            runOnUiThread {
                currentState = nextState
                renderCurrentState()
            }
        }.start()
    }

    private fun retryPushTokenSyncAsync() {
        Thread {
            val nextState = runtimeOrchestrator.retryPushTokenSync()
            runOnUiThread {
                currentState = nextState
                renderCurrentState()
            }
        }.start()
    }

    private fun subscribeTopicAsync(topic: String) {
        Thread {
            val nextState = runtimeOrchestrator.subscribeEventTopic(topic)
            runOnUiThread {
                currentState = nextState
                renderCurrentState()
            }
        }.start()
    }

    private fun sendPushSmokeTestAsync(
        topic: String,
        title: String,
        body: String
    ) {
        Thread {
            val pushNotifier = AppRuntimeComposition.providePushNotifier()
            val serverOk = pushNotifier.send(
                message = PushMessage(
                    topic = topic,
                    title = title,
                    body = body
                )
            )
            val localOk = LocalNotificationSender.trySend(
                context = this@MainActivity,
                title = "RTW Pro(카) 알림",
                body = body
            )
            val msg = when {
                serverOk && localOk -> "서버 발송 완료 + 로컬 알림 표시.(스모크)"
                serverOk && !localOk -> "서버 발송 완료(로컬 알림은 권한/환경 문제로 실패)."
                !serverOk && localOk -> "서버 발송 미구현. 로컬 알림 폴백으로 스모크 완료."
                else -> "서버 발송 미구현 + 로컬 알림도 실패(알림 권한 확인 필요)."
            }
            runOnUiThread {
                currentState = currentState.copy(
                    pushSendAttempted = true,
                    pushSendSuccess = serverOk || localOk,
                    pushSendMessage = msg
                )
                renderCurrentState()
            }
        }.start()
    }

    private fun simulateRaceEndAsync() {
        Thread {
            val nextRaceState = runCatching {
                RoomRaceStateMachine(initialState = RoomRaceState.RUNNING).onRaceEnded()
            }.getOrNull()
            // Ensure topic is subscribed (best-effort)
            val afterTopic = runtimeOrchestrator.subscribeEventTopic("daily-20h")
            val localOk = LocalNotificationSender.trySend(
                context = this@MainActivity,
                title = "RTW Pro(카) 레이스 종료",
                body = "레이스가 종료되었습니다. 결과를 확인해 주세요."
            )
            val msg = buildString {
                append("RaceEnd=")
                append(nextRaceState?.name ?: "IGNORED")
                append(" / topicSubscribed=")
                append(afterTopic.pushTopicSubscribed)
                append(" / localNoti=")
                append(if (localOk) "OK" else "FAIL")
            }
            runOnUiThread {
                currentState = afterTopic.copy(
                    pushSendAttempted = true,
                    pushSendSuccess = localOk,
                    pushSendMessage = msg
                )
                renderCurrentState()
            }
        }.start()
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
            currentState.authMessage.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                "Replace google-services.json and local.properties auth values from the same Firebase project."
            currentState.authMessage.contains("Google 로그인 설정 오류", ignoreCase = false) ->
                "Register debug SHA-1 in Firebase, use Web client OAuth ID in rtw.auth.webClientId, reinstall app."
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
            appendLine("pushSendAttempted: ${currentState.pushSendAttempted}")
            appendLine("pushSendSuccess: ${currentState.pushSendSuccess}")
            appendLine("pushSendMessage: ${currentState.pushSendMessage.ifBlank { "(empty)" }}")
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
