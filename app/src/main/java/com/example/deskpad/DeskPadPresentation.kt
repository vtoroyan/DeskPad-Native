package com.example.deskpad

import android.app.Presentation
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Companion UI rendered on a secondary presentation display.
 * This is an app-owned surface, not a system-wide overlay or input injector.
 */
class DeskPadPresentation(context: Context, display: Display) : Presentation(context, display) {

    private lateinit var pointerView: PointerDemoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 24, 32, 24)
            setBackgroundColor(0xFF101820.toInt())
        }

        root.addView(TextView(context).apply {
            text = "DeskPad Native 0.3"
            textSize = 26f
            setTextColor(0xFFE6EDF3.toInt())
            gravity = Gravity.CENTER
        })

        root.addView(TextView(context).apply {
            text = "External display connection is working"
            textSize = 15f
            setTextColor(0xFF8B949E.toInt())
            gravity = Gravity.CENTER
        })

        pointerView = PointerDemoView(context)
        root.addView(pointerView, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = 24
            bottomMargin = 24
        })

        root.addView(TextView(context).apply {
            text = "This pointer is confined to this DeskPad screen. " +
                "System-wide mouse injection is not available to an ordinary app."
            textSize = 13f
            setTextColor(0xFFE3B341.toInt())
            gravity = Gravity.CENTER
        })

        setContentView(root)
    }

    fun setPointer(x: Float, y: Float) {
        if (::pointerView.isInitialized) pointerView.setPosition(x, y)
    }

    private class PointerDemoView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var px = 0.5f
        private var py = 0.5f

        init {
            setBackgroundColor(0xFF17212B.toInt())
        }

        fun setPosition(x: Float, y: Float) {
            px = x.coerceIn(0f, 1f)
            py = y.coerceIn(0f, 1f)
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            paint.color = 0xFF58A6FF.toInt()
            canvas.drawCircle(width * px, height * py, 18f, paint)
            paint.color = 0x99FFFFFF.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawCircle(width * px, height * py, 28f, paint)
            paint.style = Paint.Style.FILL
        }
    }
}
