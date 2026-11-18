package com.example.deskpad

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.text.InputType
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
 * DeskPad Native test shell.
 *
 * The test field below is a local editor used to verify the standard Android IME.
 * The IME can type into focused text editors; it does not inject mouse events.
 */
class MainActivity : Activity(), DisplayManager.DisplayListener {

    private lateinit var displayManager: DisplayManager
    private lateinit var status: TextView
    private lateinit var pointerStatus: TextView
    private lateinit var keyboardTestStatus: TextView
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
            setPadding(20, 18, 20, 18)
            setBackgroundColor(0xFF0D1117.toInt())
        }

        status = TextView(this).apply {
            text = "Searching for an external display…"
            textSize = 16f
            setTextColor(0xFFD7DEE7.toInt())
            gravity = Gravity.CENTER
        }
        root.addView(status, LinearLayout.LayoutParams(-1, 48))

        val info = TextView(this).apply {
            text = "DeskPad Native ${BuildConfig.VERSION_NAME} · no Accessibility / no Root / no Shizuku"
            textSize = 11f
            setTextColor(0xFF8B949E.toInt())
            gravity = Gravity.CENTER
        }
        root.addView(info, LinearLayout.LayoutParams(-1, 34))

        val testTitle = TextView(this).apply {
            text = "KEYBOARD TEST"
            textSize = 12f
            setTextColor(0xFF58A6FF.toInt())
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }
        root.addView(testTitle, LinearLayout.LayoutParams(-1, 28))

        val testInput = EditText(this).apply {
            hint = "Tap here, then type with DeskPad Keyboard"
            textSize = 15f
            singleLine = false
            minLines = 2
            maxLines = 3
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF8B949E.toInt())
            setPadding(12, 8, 12, 8)
            setBackgroundColor(0xFF202938.toInt())
        }
        root.addView(testInput, LinearLayout.LayoutParams(-1, 76))

        keyboardTestStatus = TextView(this).apply {
            text = "Status: test field is not focused"
            textSize = 11f
            setTextColor(0xFF8B949E.toInt())
            gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(keyboardTestStatus, LinearLayout.LayoutParams(-1, 28))

        testInput.setOnFocusChangeListener { _, hasFocus ->
            keyboardTestStatus.text = if (hasFocus) {
                "Status: test field focused · select DeskPad Keyboard if needed"
            } else {
                "Status: test field is not focused"
            }
        }

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val chooseIme = Button(this).apply {
            text = "Choose keyboard"
            textSize = 11f
            setOnClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        }
        buttons.addView(chooseIme, LinearLayout.LayoutParams(0, 48, 1f).apply {
            setMargins(2, 2, 4, 2)
        })

        val openIme = Button(this).apply {
            text = "Open test keyboard"
            textSize = 11f
            setOnClickListener {
                testInput.requestFocus()
                testInput.post {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(testInput, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }
        buttons.addView(openIme, LinearLayout.LayoutParams(0, 48, 1f).apply {
            setMargins(4, 2, 2, 2)
        })
        root.addView(buttons, LinearLayout.LayoutParams(-1, 52))

        val pad = TouchpadView(this).apply {
            setBackgroundColor(0xFF161B22.toInt())
            onPadFrame = ::onPadFrame
        }
        root.addView(pad, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = 10
            bottomMargin = 8
        })

        pointerStatus = TextView(this).apply {
            text = "Touch the pad to move the demo pointer on the external display."
            textSize = 11f
            setTextColor(0xFF8B949E.toInt())
            gravity = Gravity.CENTER
        }
        root.addView(pointerStatus, LinearLayout.LayoutParams(-1, 36))

        val note = TextView(this).apply {
            text = "Pointer is only a demo inside DeskPad's presentation. It cannot click or control other apps."
            textSize = 10f
            setTextColor(0xFFE3B341.toInt())
            gravity = Gravity.CENTER
            setPadding(6, 6, 6, 0)
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
