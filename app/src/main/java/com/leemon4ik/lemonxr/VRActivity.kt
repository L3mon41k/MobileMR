
package com.leemon4ik.lemonxr

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import kotlin.math.cos
import kotlin.math.sin

class VRActivity : Activity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private lateinit var vrView: VRView

    private var yaw = 0f
    private var pitch = 0f
    private var roll = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(
            Sensor.TYPE_GAME_ROTATION_VECTOR
        ) ?: sensorManager.getDefaultSensor(
            Sensor.TYPE_ROTATION_VECTOR
        )

        vrView = VRView()
        setContentView(vrView)
    }

    override fun onResume() {
        super.onResume()
        rotationSensor?.let {
            sensorManager.registerListener(
                this, it, SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GAME_ROTATION_VECTOR ||
            event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR
        ) {
            val matrix = FloatArray(9)
            val angles = FloatArray(3)

            SensorManager.getRotationMatrixFromVector(matrix, event.values)
            SensorManager.getOrientation(matrix, angles)

            yaw = angles[0]
            pitch = angles[1]
            roll = angles[2]

            vrView.invalidate()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private inner class VRView : View(this@VRActivity) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            canvas.drawColor(Color.BLACK)

            val halfWidth = width / 2f
            val centerY = height / 2f

            drawEye(canvas, 0f, halfWidth, centerY, -1f)
            drawEye(canvas, halfWidth, halfWidth, centerY, 1f)

            paint.color = Color.WHITE
            paint.strokeWidth = 2f
            canvas.drawLine(
                halfWidth, 0f, halfWidth, height.toFloat(), paint
            )
        }

        private fun drawEye(
            canvas: Canvas,
            offsetX: Float,
            eyeWidth: Float,
            centerY: Float,
            eyeSide: Float
        ) {
            canvas.save()
            canvas.translate(offsetX, 0f)

            val centerX = eyeWidth / 2f

            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f

            val radius = 42f
            val shiftX = (sin(yaw) * 35f) + eyeSide * 8f
            val shiftY = sin(pitch) * 35f

            canvas.drawCircle(
                centerX + shiftX,
                centerY + shiftY,
                radius,
                paint
            )

            paint.style = Paint.Style.FILL
            paint.textSize = 22f
            paint.textAlign = Paint.Align.CENTER

            canvas.drawText(
                "LEMONXR",
                centerX + shiftX,
                centerY + shiftY - 65f,
                paint
            )

            paint.textSize = 13f
            canvas.drawText(
                "VR PROTOTYPE",
                centerX + shiftX,
                centerY + shiftY + 75f,
                paint
            )

            paint.textSize = 11f
            canvas.drawText(
                "HEAD TRACKING",
                centerX,
                height - 35f,
                paint
            )

            canvas.restore()
        }
    }
}
