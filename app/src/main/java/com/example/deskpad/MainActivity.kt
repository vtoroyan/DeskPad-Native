package com.example.deskpad

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.example.deskpad.core.PadFrame
import com.example.deskpad.core.PadPhase

/**
 * Native 0.3 first-stage shell.
 *
 * This Activity deliberately has no AccessibilityService dependency. It can discover a
 * presentation display and render a local pointer demo there, but it cannot inject input
 * into other applications. That requires a privileged/system input mechanism.
 */
class MainActivity : Activity(), DisplayManager.DisplayListener {

    private lateinit var displayManager: DisplayManager
    private lateinit var status: TextView
    private lateinit var pointerStatus: TextView
    private var presentation: DeskPadPresentation? = null
    private var lastPadX = Float.NaN
    private var lastPadY = Float.NaN
    private var pointerX = 0.5f
    private var pointerY = 0.5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        setContentView(buildUi())
        displayManager.registerDisplayListener(this, null)
        refreshPresentation()
    }

    override fun onDestroy() {
        displayManager.unregisterDisplayListener(this)
        presentation?.dismiss()
        presentation = null
        super.onDestroy()
    }

    override fun onDisplayAdded(displayId: Int) = refreshPresentation()
    override fun onDisplayRemoved(displayId: Int) = refreshPresentation()
    override fun onDisplayChanged(displayId: Int) = refreshPresentation()

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFF0D1117.toInt())
        }

        status = TextView(this).apply {
            text = "Searching for an external display…"
            textSize = 16f
            setTextColor(0xFFD7DEE7.toInt())
            gravity = Gravity.CENTER
        }
        root.addView(status, LinearLayout.LayoutParams(-1, 64))

        val info = TextView(this).apply {
            text = "DeskPad Native 0.3 · no Accessibility / no Root / no Shizuku"
            textSize = 12f
            setTextColor(0xFF8B949E.toInt())
            gravity = Gravity.CENTER
        }
        root.addView(info, LinearLayout.LayoutParams(-1, 48))

        val pad = TouchpadView(this).apply {
            setBackgroundColor(0xFF161B22.toInt())
            onPadFrame = ::onPadFrame
        }
        root.addView(pad, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = 16
            bottomMargin = 16
        })

        pointerStatus = TextView(this).apply {
            text = "Touch the pad to move the demo pointer on the external display."
            textSize = 12f
            setTextColor(0xFF8B949E.toInt())
            gravity = Gravity.CENTER
        }
        root.addView(pointerStatus, LinearLayout.LayoutParams(-1, 44))

        val imeButton = Button(this).apply {
            text = "Choose keyboard (IME)"
            setOnClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        }
        root.addView(imeButton, LinearLayout.LayoutParams(-1, 52))

        val note = TextView(this).apply {
            text = "The pointer is currently a demo inside DeskPad's own presentation. " +
                "It does not click or control other apps."
            textSize = 11f
            setTextColor(0xFFE3B341.toInt())
            gravity = Gravity.CENTER
            setPadding(8, 12, 8, 0)
        }
        root.addView(note, LinearLayout.LayoutParams(-1, -2))
        return root
    }

    private fun onPadFrame(frame: PadFrame) {
        if (frame.phase == PadPhase.DOWN) {
            lastPadX = frame.x
            lastPadY = frame.y
            return
        }
        if (frame.phase == PadPhase.MOVE) {
            if (!lastPadX.isNaN()) {
                val dx = (frame.x - lastPadX) / 700f
                val dy = (frame.y - lastPadY) / 700f
                pointerX = (pointerX + dx).coerceIn(0.02f, 0.98f)
                pointerY = (pointerY + dy).coerceIn(0.02f, 0.98f)
                presentation?.setPointer(pointerX, pointerY)
            }
            lastPadX = frame.x
            lastPadY = frame.y
        } else if (frame.phase == PadPhase.UP) {
            lastPadX = Float.NaN
            lastPadY = Float.NaN
        }
    }

    private fun refreshPresentation() {
        val external = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .firstOrNull { it.displayId != 0 }

        if (external == null) {
            presentation?.dismiss()
            presentation = null
            status.text = "No presentation display detected"
            pointerStatus.text = "Connect an external display and enable Desktop Mode."
            return
        }

        if (presentation?.display?.displayId != external.displayId) {
            presentation?.dismiss()
            presentation = DeskPadPresentation(this, external).also { it.show() }
        }
        status.text = "External display detected · ${external.name}"
        pointerStatus.text = "Demo pointer: ${(pointerX * 100).toInt()}%, ${(pointerY * 100).toInt()}%"
        presentation?.setPointer(pointerX, pointerY)
    }
}
