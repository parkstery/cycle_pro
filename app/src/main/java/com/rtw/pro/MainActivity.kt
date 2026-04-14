package com.rtw.pro

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            TextView(this).apply {
                text = "Ride The World Pro\nAndroid runtime wired."
                textSize = 20f
                setPadding(48, 96, 48, 48)
            }
        )
    }
}
