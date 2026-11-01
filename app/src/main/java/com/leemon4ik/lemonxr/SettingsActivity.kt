package com.leemon4ik.lemonxr

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val preferences =
            getSharedPreferences("lemonxr_settings", MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            setBackgroundColor(Color.BLACK)
        }

        val title = TextView(this).apply {
            text = "SETTINGS"
            textSize = 26f
            letterSpacing = 0.12f
            setTextColor(Color.WHITE)
        }

        root.addView(title, params(dp(56)))

        addSection(root, "CONTROL MODE")

        val controlMode = addSpinner(
            root,
            listOf(
                "Headset button",
                "Hand tracking",
                "Combined"
            )
        )

        addSection(root, "CAMERA")

        val camera = addSpinner(
            root,
            listOf(
                "System default",
                "Rear main",
                "Rear ultra-wide",
                "Front"
            )
        )

        addSection(root, "HAND TRACKING")

        val info = TextView(this).apply {
            text = "Hand tracking options will appear here."
            textSize = 13f
            setTextColor(Color.LTGRAY)
        }

        root.addView(info, params(dp(56)))

        controlMode.setSelection(
            preferences.getInt("control_mode", 0).coerceIn(0, 2)
        )

        camera.setSelection(
            preferences.getInt("camera", 0).coerceIn(0, 3)
        )

        controlMode.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    preferences.edit()
                        .putInt("control_mode", position)
                        .apply()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        camera.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    preferences.edit()
                        .putInt("camera", position)
                        .apply()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        val spacer = View(this)
        root.addView(
            spacer,
            LinearLayout.LayoutParams(1, 0, 1f)
        )

        val back = TextView(this).apply {
            text = "DONE"
            textSize = 14f
            letterSpacing = 0.14f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
            setOnClickListener { finish() }
        }

        root.addView(back, params(dp(54)))

        setContentView(root)
    }

    private fun addSection(root: LinearLayout, label: String) {
        val text = TextView(this).apply {
            this.text = label
            textSize = 11f
            letterSpacing = 0.16f
            setTextColor(Color.GRAY)
        }

        root.addView(text, params(dp(34)))
    }

    private fun addSpinner(
        root: LinearLayout,
        values: List<String>
    ): Spinner {

        val spinner = Spinner(this)

        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            values
        )

        spinner.setBackgroundColor(Color.WHITE)

        root.addView(spinner, params(dp(52)))
        root.addView(View(this), params(dp(22)))

        return spinner
    }

    private fun params(height: Int) =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            height
        )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
