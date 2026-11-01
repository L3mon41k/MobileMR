
package com.leemon4ik.lemonxr

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.RadioGroup
import android.widget.RadioButton
import android.widget.ScrollView
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics

class SettingsActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("lemonxr_settings", MODE_PRIVATE)
    }

    private val white = Color.WHITE
    private val gray = Color.rgb(145, 145, 145)
    private val dark = Color.rgb(12, 12, 12)
    private val border = Color.rgb(55, 55, 55)

    private data class CameraOption(
        val id: String,
        val name: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(30), dp(22), dp(32))
            setBackgroundColor(Color.BLACK)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }

        val title = TextView(this).apply {
            text = "SETTINGS"
            textSize = 27f
            letterSpacing = 0.12f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
        }

        content.addView(title)
        content.addView(space(5))

        val subtitle = TextView(this).apply {
            text = "Configure your LemonXR experience"
            textSize = 13f
            setTextColor(gray)
        }

        content.addView(subtitle)
        content.addView(space(30))

        addSection(content, "CONTROL")

        val modes = listOf(
            "Headset button" to "headset",
            "Hand tracking" to "hands",
            "Combined" to "combined"
        )

        val modeCard = createCard()
        val modeGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }

        val modeIds = mutableMapOf<String, Int>()

        modes.forEachIndexed { index, item ->
            val radio = createRadio(item.first)
            modeIds[item.second] = radio.id
            modeGroup.addView(radio)

            if (index != modes.lastIndex) {
                modeGroup.addView(separator())
            }
        }

        val savedMode = prefs.getString("control_mode", "headset")
        modeGroup.check(modeIds[savedMode] ?: modeIds["headset"]!!)

        modeGroup.setOnCheckedChangeListener { _, checkedId ->
            val selected = modes.firstOrNull {
                modeIds[it.second] == checkedId
            }

            selected?.let {
                prefs.edit()
                    .putString("control_mode", it.second)
                    .apply()
            }
        }

        modeCard.addView(modeGroup)
        content.addView(modeCard)

        content.addView(space(28))
        addSection(content, "CAMERA")

        val cameraCard = createCard()
        val cameraGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }

        try {
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
            val options = mutableListOf<CameraOption>()

            manager.cameraIdList.forEach { id ->
                val characteristics =
                    manager.getCameraCharacteristics(id)

                val facing = characteristics.get(
                    CameraCharacteristics.LENS_FACING
                )

                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    val focalLengths = characteristics.get(
                        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                    )

                    val focal = focalLengths?.firstOrNull()

                    val name = when {
                        focal == null -> "Rear camera"
                        focal <= 2.4f -> "Ultra-wide"
                        focal >= 7f -> "Telephoto"
                        else -> "Wide"
                    }

                    options.add(CameraOption(id, name))
                }
            }

            if (options.isEmpty()) {
                cameraCard.addView(infoText("No rear cameras found."))
            } else {
                val counts = mutableMapOf<String, Int>()

                options.forEachIndexed { index, camera ->
                    val count = (counts[camera.name] ?: 0) + 1
                    counts[camera.name] = count

                    val label = if (count == 1) {
                        camera.name
                    } else {
                        "${camera.name} $count"
                    }

                    val radio = createRadio(label)
                    radio.tag = camera.id

                    radio.setOnClickListener {
                        prefs.edit()
                            .putString("selected_camera", camera.id)
                            .apply()
                    }

                    cameraGroup.addView(radio)

                    if (camera.id == prefs.getString(
                            "selected_camera", null
                        )
                    ) {
                        cameraGroup.check(radio.id)
                    }

                    if (index != options.lastIndex) {
                        cameraGroup.addView(separator())
                    }
                }

                if (cameraGroup.checkedRadioButtonId == -1) {
                    val first = cameraGroup.getChildAt(0) as RadioButton
                    cameraGroup.check(first.id)

                    prefs.edit()
                        .putString("selected_camera", first.tag as String)
                        .apply()
                }

                cameraCard.addView(cameraGroup)
            }
        } catch (e: Exception) {
            cameraCard.addView(
                infoText("Unable to detect rear cameras.")
            )
        }

        content.addView(cameraCard)

        content.addView(space(28))
        addSection(content, "ABOUT")

        val aboutCard = createCard()

        val version = TextView(this).apply {
            text = "LEMONXR"
            textSize = 16f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
        }

        val versionInfo = TextView(this).apply {
            text = "BETA 0.1.1"
            textSize = 12f
            setTextColor(gray)
        }

        val description = TextView(this).apply {
            text = "Mobile virtual reality environment"
            textSize = 13f
            setTextColor(gray)
        }

        aboutCard.addView(version)
        aboutCard.addView(space(5))
        aboutCard.addView(versionInfo)
        aboutCard.addView(space(12))
        aboutCard.addView(separator())
        aboutCard.addView(space(12))
        aboutCard.addView(description)

        content.addView(aboutCard)
        content.addView(space(25))

        val footer = TextView(this).apply {
            text = "LEMONXR  •  VIRTUAL REALITY"
            textSize = 10f
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(90, 90, 90))
        }

        content.addView(footer)

        setContentView(scroll)
    }

    private fun addSection(root: LinearLayout, label: String) {
        val text = TextView(this).apply {
            this.text = label
            textSize = 11f
            letterSpacing = 0.16f
            setTextColor(gray)
            typeface = Typeface.DEFAULT_BOLD
        }

        root.addView(text)
        root.addView(space(12))
    }

    private fun createCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(10), dp(15), dp(10))

        background = GradientDrawable().apply {
            setColor(dark)
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), border)
        }
    }

    private fun createRadio(label: String) =
        RadioButton(this).apply {
            text = label
            textSize = 14f
            setTextColor(white)
            buttonTintList =
                android.content.res.ColorStateList.valueOf(white)
            id = View.generateViewId()
            minHeight = dp(52)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), 0, dp(5), 0)
        }

    private fun infoText(value: String) =
        TextView(this).apply {
            text = value
            textSize = 13f
            setTextColor(gray)
            setPadding(dp(5), dp(15), dp(5), dp(15))
        }

    private fun separator() = View(this).apply {
        setBackgroundColor(border)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(1)
        )
    }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            1, dp(height)
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
