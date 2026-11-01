
package com.leemon4ik.lemonxr

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(32), dp(28), dp(32))
            setBackgroundColor(Color.BLACK)
        }

        val title = TextView(this).apply {
            text = "LEMONXR"
            textSize = 34f
            letterSpacing = 0.18f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "MOBILE VR PLATFORM"
            textSize = 11f
            letterSpacing = 0.24f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        val settingsButton = createButton("SETTINGS")
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val launchButton = createButton("LAUNCH")
        launchButton.setOnClickListener {
    startActivity(Intent(this, VRActivity::class.java))
        }

        root.addView(title, matchWrap())
        root.addView(subtitle, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(30)
        ))
        root.addView(spacer(48))
        root.addView(settingsButton, buttonParams())
        root.addView(spacer(16))
        root.addView(launchButton, buttonParams())

        setContentView(root)
    }

    private fun createButton(label: String) = Button(this).apply {
        text = label
        textSize = 14f
        letterSpacing = 0.12f
        setTextColor(Color.BLACK)
        setBackgroundColor(Color.WHITE)
        isAllCaps = false
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        minHeight = dp(58)
        stateListAnimator = null
    }

    private fun buttonParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, dp(58)
    )

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun spacer(height: Int) = android.view.View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
