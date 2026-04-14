package com.rtw.pro

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rtw.pro.app.AppRuntimeComposition

class MainActivity : AppCompatActivity() {
    private lateinit var dashboardText: TextView

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
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 32)
            addView(refreshButton)
            addView(dashboardText)
        }
        setContentView(ScrollView(this).apply { addView(layout) })

        renderRuntimeState()
    }

    private fun renderRuntimeState() {
        val startupHandler = AppRuntimeComposition.provideMainAppStartupHandler(applicationContext)
        val runtimeState = startupHandler.onCreate(
            mapConfig = AppRuntimeComposition.defaultMapConfig(),
            streetViewConfig = AppRuntimeComposition.defaultStreetViewConfig()
        )

        dashboardText.text = buildString {
            appendLine("Ride The World Pro")
            appendLine("Runtime status dashboard")
            appendLine()
            appendLine("authReady: ${runtimeState.authReady}")
            appendLine("authStatus: ${runtimeState.authStatus}")
            appendLine("authMessage: ${runtimeState.authMessage.ifBlank { "(empty)" }}")
            appendLine("mapReady: ${runtimeState.mapReady}")
            appendLine("mapMode: ${runtimeState.mapMode}")
            appendLine("mapErrorCode: ${runtimeState.mapErrorCode ?: "none"}")
            appendLine("pushTokenSynced: ${runtimeState.pushTokenSynced}")
            appendLine()
            appendLine("mapMessage:")
            appendLine(runtimeState.mapMessage.ifBlank { "(empty)" })
            appendLine()
            appendLine("refreshTimeMs: ${System.currentTimeMillis()}")
        }
    }
}
