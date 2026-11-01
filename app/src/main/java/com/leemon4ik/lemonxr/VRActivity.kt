package com.leemon4ik.lemonxr

import android.app.Activity
import android.os.Bundle
import android.os.BatteryManager
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VRActivity : Activity() {

    private lateinit var vrView: VRView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        vrView = VRView()
        setContentView(vrView)
    }

    inner class VRView : View(this) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val apps = listOf(
            "Browser", "Camera", "Calculator",
            "Calendar", "Notes", "All Apps"
        )

        private val icons = listOf("B", "C", "=", "31", "N", "▦")

        private var selectedApp = -1

        private val white = Color.WHITE
        private val gray = Color.rgb(155, 155, 155)
        private val dark = Color.rgb(18, 18, 18)
        private val line = Color.rgb(65, 65, 65)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            canvas.drawColor(Color.BLACK)

            val half = width / 2f
            if (half <= 0f || height <= 0) return

            drawEye(canvas, 0f, half)
            drawEye(canvas, half, half)

            postInvalidateDelayed(1000)
        }

        private fun drawEye(canvas: Canvas, offset: Float, eyeWidth: Float) {
            val save = canvas.save()
            canvas.translate(offset, 0f)

            val scale = eyeWidth / 480f
            canvas.scale(scale, height / 480f)

            val w = 480f
            val h = 480f * height / eyeWidth

            paint.color = white
            paint.textSize = 25f
            paint.typeface = android.graphics.Typeface.create(
                "sans-serif-medium",
                android.graphics.Typeface.NORMAL
            )
            canvas.drawText("LEMONXR", 28f, 45f, paint)

            paint.color = gray
            paint.textSize = 13f
            canvas.drawText("VIRTUAL ENVIRONMENT", 28f, 65f, paint)

            val time = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date())
            val date = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
                .format(Date())

            val batteryIntent = registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            val level = batteryIntent?.getIntExtra(
                BatteryManager.EXTRA_LEVEL, -1
            ) ?: -1

            val batteryText = if (level >= 0) "$level%" else "--%"

            paint.color = white
            paint.textSize = 24f
            canvas.drawText(time, w - 125f, 43f, paint)

            paint.color = gray
            paint.textSize = 12f
            canvas.drawText("$date  •  $batteryText", w - 125f, 64f, paint)

            paint.color = line
            paint.strokeWidth = 1f
            canvas.drawLine(28f, 86f, w - 28f, 86f, paint)

            paint.color = white
            paint.textSize = 17f
            canvas.drawText("APPLICATIONS", 28f, 125f, paint)

            val cardWidth = 118f
            val cardHeight = 112f
            val gap = 12f
            val startX = 28f
            val startY = 145f

            for (i in apps.indices) {
                val x = startX + i * (cardWidth + gap)
                val rect = RectF(
                    x, startY,
                    x + cardWidth, startY + cardHeight
                )

                paint.color = if (i == selectedApp) {
                    Color.rgb(55, 55, 55)
                } else {
                    dark
                }

                canvas.drawRoundRect(rect, 12f, 12f, paint)

                paint.color = line
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(rect, 12f, 12f, paint)
                paint.style = Paint.Style.FILL

                paint.color = white
                paint.textSize = 30f
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD

                val iconWidth = paint.measureText(icons[i])
                canvas.drawText(
                    icons[i],
                    x + (cardWidth - iconWidth) / 2f,
                    startY + 52f,
                    paint
                )

                paint.color = white
                paint.textSize = 12f
                paint.typeface = android.graphics.Typeface.DEFAULT

                val nameWidth = paint.measureText(apps[i])
                canvas.drawText(
                    apps[i],
                    x + (cardWidth - nameWidth) / 2f,
                    startY + 88f,
                    paint
                )
            }

            paint.color = line
            canvas.drawLine(28f, startY + 140f, w - 28f, startY + 140f, paint)

            paint.color = gray
            paint.textSize = 12f
            canvas.drawText(
                "LEMONXR  •  BETA 0.1.1",
                28f,
                h - 25f,
                paint
            )

            canvas.restoreToCount(save)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                val eyeWidth = width / 2f
                if (eyeWidth <= 0f) return true

                val localX = (event.x % eyeWidth) / (eyeWidth / 480f)
                val localY = event.y / (height / 480f)

                val cardWidth = 118f
                val gap = 12f
                val startX = 28f
                val startY = 145f

                if (localY in startY..(startY + 112f)) {
                    val index = ((localX - startX) / (cardWidth + gap)).toInt()

                    if (index in apps.indices &&
                        localX >= startX + index * (cardWidth + gap) &&
                        localX <= startX + index * (cardWidth + gap) + cardWidth
                    ) {
                        selectedApp = index
                        val message = when (index) {
                            0 -> "Browser"
                            1 -> "Camera — coming soon"
                            2 -> "Calculator — coming soon"
                            3 -> "Calendar — coming soon"
                            4 -> "Notes — coming soon"
                            else -> "All Apps — coming soon"
                        }

                        Toast.makeText(
                            context,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()

                        invalidate()
                    }
                }
            }

            return true
        }
    }
}
