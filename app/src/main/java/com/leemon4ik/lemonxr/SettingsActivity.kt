
package com.leemon4ik.lemonxr

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.Gravity
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
            setPadding(dp(28), dp(36), dp(28), dp(36))
            setBackgroundColor(Color.BLACK)
        }

        val scroll = ScrollView(this).apply {
            addView(content)
        }

        val title = TextView(this).apply {
            text = "SETTINGS"
            textSize = 28f
            letterSpacing = 0.15f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        content.addView(title)
        content.addView(space(32))

        addSection(content, "CONTROL MODE")

        val modes = listOf(
            "Headset button" to "headset",
            "Hand tracking" to "hands",
            "Combined" to "combined"
        )

        val modeGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }

        val modeIds = mutableMapOf<String, Int>()

        modes.forEach { (label, key) ->
            val radio = RadioButton(this).apply {
                text = label
                textSize = 16f
                setTextColor(Color.WHITE)
                buttonTintList =
                    android.content.res.ColorStateList.valueOf(Color.WHITE)
                id = View.generateViewId()
            }

            modeIds[key] = radio.id
            modeGroup.addView(radio)
        }

        val savedMode = prefs.getString("control_mode", "headset")
        modeGroup.check(modeIds[savedMode] ?: modeIds["headset"]!!)

        modeGroup.setOnCheckedChangeListener { group, checkedId ->
            val selected = modes.firstOrNull {
                modeIds[it.second] == checkedId
            }
            selected?.let {
                prefs.edit().putString("control_mode", it.second).apply()
            }
        }

        content.addView(modeGroup)
        content.addView(space(30))

        addSection(content, "REAR CAMERA")

        val cameraGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }

        val cameraInfo = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.LTGRAY)
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
                        focal == null -> "Back Camera"
                        focal <= 2.4f -> "Ultra-wide"
                        focal >= 7f -> "Telephoto"
                        else -> "Wide"
                    }

                    options.add(CameraOption(id, name))
                }
            }

            if (options.isEmpty()) {
                cameraInfo.text = "No rear cameras found."
                content.addView(cameraInfo)
            } else {
                val counts = mutableMapOf<String, Int>()

                options.forEach { camera ->
                    val count = (counts[camera.name] ?: 0) + 1
                    counts[camera.name] = count

                    val label = if (count == 1) {
                        camera.name
                    } else {
                        "${camera.name} $count"
                    }

                    val radio = RadioButton(this).apply {
                        text = label
                        textSize = 16f
                        setTextColor(Color.WHITE)
                        buttonTintList =
                            android.content.res.ColorStateList.valueOf(Color.WHITE)
                        id = View.generateViewId()
                    }

                    radio.tag = camera.id

                    radio.setOnClickListener {
                        prefs.edit()
                            .putString("selected_camera", camera.id)
                            .apply()
                    }

                    cameraGroup.addView(radio)

                    if (camera.id == prefs.getString("selected_camera", null)) {
                        cameraGroup.check(radio.id)
                    }
                }

                if (cameraGroup.checkedRadioButtonId == -1) {
                    val first = cameraGroup.getChildAt(0) as RadioButton
                    cameraGroup.check(first.id)
                    prefs.edit()
                        .putString("selected_camera", first.tag as String)
                        .apply()
                }

                content.addView(cameraGroup)
            }

        } catch (e: Exception) {
            cameraInfo.text = "Unable to detect rear cameras."
            content.addView(cameraInfo)
        }

        content.addView(space(30))

        val note = TextView(this).apply {
            text = "Camera choice is saved automatically.\nMore VR settings coming soon."
            textSize = 12f
            setTextColor(Color.GRAY)
        }

        content.addView(note)

        setContentView(scroll)
    }

    private fun addSection(root: LinearLayout, text: String) {
        val label = TextView(this).apply {
            this.text = text
            textSize = 12f
            letterSpacing = 0.15f
            setTextColor(Color.GRAY)
            typeface = Typeface.DEFAULT_BOLD
        }

        root.addView(label)
        root.addView(space(12))
    }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
