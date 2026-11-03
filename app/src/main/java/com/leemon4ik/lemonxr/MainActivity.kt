package com.leemon4ik.lemonxr

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private val white = Color.WHITE
    private val gray = Color.rgb(150, 150, 150)
    private val black = Color.BLACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = black
        window.navigationBarColor = black

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(30), dp(28), dp(30))
            setBackgroundColor(black)
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val logo = TextView(this).apply {
            text = "LEMONXR"
            textSize = 38f
            letterSpacing = 0.18f
            setTextColor(white)
            typeface = Typeface.create(
                "sans-serif-medium",
                Typeface.NORMAL
            )
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "MOBILE VIRTUAL REALITY"
            textSize = 11f
            letterSpacing = 0.25f
            setTextColor(gray)
            gravity = Gravity.CENTER
        }

        top.addView(logo, matchWrap())
        top.addView(subtitle, matchWrap())

        val version = TextView(this).apply {
            text = "BETA 0.1.1"
            textSize = 11f
            letterSpacing = 0.15f
            setTextColor(gray)
            gravity = Gravity.CENTER
        }

        val separator = View(this).apply {
            setBackgroundColor(Color.rgb(55, 55, 55))
        }

        val launch = createButton("LAUNCH", true).apply {
            setOnClickListener {
                startActivity(Intent(this@MainActivity, VRActivity::class.java))
            }
        }

        val settings = createButton("SETTINGS", false).apply {
            setOnClickListener {
                startActivity(
                    Intent(this@MainActivity, SettingsActivity::class.java)
                )
            }
        }

        val footer = TextView(this).apply {
            text = "DESIGNED FOR IMMERSIVE EXPERIENCES"
            textSize = 9f
            letterSpacing = 0.16f
            setTextColor(Color.rgb(95, 95, 95))
            gravity = Gravity.CENTER
        }

        root.addView(top, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        root.addView(space(18))

        root.addView(version, matchWrap())

        root.addView(space(34))

        root.addView(separator, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(1)
        ))

        root.addView(space(34))

        root.addView(launch, buttonParams())

        root.addView(space(14))

        root.addView(settings, buttonParams())

        root.addView(space(40))

        root.addView(footer, matchWrap())

        setContentView(root)
    }

    private fun createButton(label: String, primary: Boolean): TextView {
        return TextView(this).apply {
            text = label
            textSize = 13f
            letterSpacing = 0.16f
            gravity = Gravity.CENTER
            setTextColor(if (primary) black else white)
            typeface = Typeface.create(
                "sans-serif-medium",
                Typeface.NORMAL
            )

            background = GradientDrawable().apply {
                setColor(if (primary) white else black)
                cornerRadius = dp(10).toFloat()

                if (!primary) {
                    setStroke(dp(1), Color.rgb(75, 75, 75))
                }
            }

            isClickable = true
            isFocusable = true
        }
    }

    private fun buttonParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        dp(58)
    )

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
