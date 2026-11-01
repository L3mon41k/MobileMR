
package com.leemon4ik.lemonxr

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.RadioGroup
import android.widget.RadioButton
import android.hardware.camera2.CameraManager
import android.widget.Toast

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 36)
            setBackgroundColor(Color.BLACK)
        }

        val title = TextView(this).apply {
            text = "SETTINGS"
            textSize = 28f
            letterSpacing = 0.15f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        root.addView(title)
        root.addView(space(32))

        addSection(root, "CONTROL MODE")

        val modes = listOf(
            "Headset button",
            "Hand tracking",
            "Combined"
        )

        val group = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }

        modes.forEach { mode ->
            val radio = RadioButton(this).apply {
                text = mode
                textSize = 16f
                setTextColor(Color.WHITE)
                buttonTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
                id = ViewId.generate()
            }
            group.addView(radio)
        }

        group.check(group.getChildAt(0).id)

        root.addView(group)
        root.addView(space(30))

        addSection(root, "CAMERAS")

        val cameraInfo = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.LTGRAY)
        }

        try {
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
            val cameras = manager.cameraIdList

            cameraInfo.text = if (cameras.isEmpty()) {
                "No cameras found"
            } else {
                cameras.mapIndexed { index, id ->
                    val characteristics = manager.getCameraCharacteristics(id)
                    val facing = characteristics.get(
                        android.hardware.camera2.CameraCharacteristics.LENS_FACING
                    )

                    val name = when (facing) {
                        android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT ->
                            "Front camera"
                        android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK ->
                            "Back camera"
                        else -> "External camera"
                    }

                    "${index + 1}. $name (ID: $id)"
                }.joinToString("\n\n")
            }
        } catch (e: Exception) {
            cameraInfo.text = "Unable to access cameras"
        }

        root.addView(cameraInfo)

        root.addView(space(32))

        val note = TextView(this).apply {
            text = "More VR settings coming soon."
            textSize = 12f
            setTextColor(Color.GRAY)
        }

        root.addView(note)

        setContentView(root)
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

    private fun space(height: Int) = android.view.View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, height)
    }

    private object ViewId {
        private var next = 1
        fun generate(): Int = next++
    }
}

