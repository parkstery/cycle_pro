package com.rtw.pro

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.rtw.pro.BuildConfig
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
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 32)
            addView(refreshButton)
            addView(retryPushTokenButton)
            addView(retryAuthButton)
            addView(signOutButton)
            addView(googleSignInButton)
            addView(subscribeTopicButton)
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
        dashboardText.text = buildString {
            appendLine("Ride The World Pro")
            appendLine("Runtime status dashboard")
            appendLine()
            appendLine("authConfigLooksReal: $authConfigLooksReal")
            appendLine("googleServicesJsonPresent: ${BuildConfig.HAS_GOOGLE_SERVICES_JSON}")
            appendLine("authReady: ${currentState.authReady}")
            appendLine("authStatus: ${currentState.authStatus}")
            appendLine("authMessage: ${currentState.authMessage.ifBlank { "(empty)" }}")
            appendLine("mapReady: ${currentState.mapReady}")
            appendLine("mapMode: ${currentState.mapMode}")
            appendLine("mapErrorCode: ${currentState.mapErrorCode ?: "none"}")
            appendLine("pushTokenSynced: ${currentState.pushTokenSynced}")
            appendLine("pushTokenErrorCode: ${currentState.pushTokenErrorCode ?: "none"}")
            appendLine("pushTopicSubscribed: ${currentState.pushTopicSubscribed}")
            appendLine("pushTopic: ${currentState.pushTopic.ifBlank { "(empty)" }}")
            appendLine("pushTopicErrorCode: ${currentState.pushTopicErrorCode ?: "none"}")
            appendLine()
            appendLine("mapMessage:")
            appendLine(currentState.mapMessage.ifBlank { "(empty)" })
            appendLine()
            appendLine("refreshTimeMs: ${System.currentTimeMillis()}")
        }
    }
}
