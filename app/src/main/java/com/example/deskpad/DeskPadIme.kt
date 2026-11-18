package com.example.deskpad

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * A compact on-screen keyboard for testing DeskPad's standard IME connection.
 *
 * This can type only into the editor currently connected to Android's InputMethodService.
 * It does not inject mouse events or control arbitrary UI elements.
 */
class DeskPadIme : InputMethodService() {

    private var active = false
    private var shifted = false
    private lateinit var statusView: TextView

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setBackgroundColor(0xFF101820.toInt())
        }

        statusView = TextView(this).apply {
            text = "DeskPad keyboard · waiting for editor"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(0xFF9FB3C8.toInt())
            setPadding(0, dp(3), 0, dp(7))
        }
        root.addView(statusView, LinearLayout.LayoutParams(-1, dp(28)))

        addRow(root, listOf("1","2","3","4","5","6","7","8","9","0"))
        addRow(root, listOf("q","w","e","r","t","y","u","i","o","p"))
        addRow(root, listOf("a","s","d","f","g","h","j","k","l"))
        addRow(root, listOf("⇧","z","x","c","v","b","n","m","⌫"), special = true)
        addBottomRow(root)

        return root
    }

    private fun addRow(parent: LinearLayout, keys: List<String>, special: Boolean = false) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        for (key in keys) {
            val button = keyButton(key)
            val weight = if (special && (key == "⇧" || key == "⌫")) 1.45f else 1f
            row.addView(button, LinearLayout.LayoutParams(0, dp(44), weight).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            })
        }
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(48)))
    }

    private fun addBottomRow(parent: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        row.addView(keyButton("?123") {
            commitText("?123")
        }, LinearLayout.LayoutParams(0, dp(46), 1.3f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        row.addView(keyButton(",") { commitText(",") },
            LinearLayout.LayoutParams(0, dp(46), 0.8f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        row.addView(keyButton("Space") { commitText(" ") },
            LinearLayout.LayoutParams(0, dp(46), 4f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        row.addView(keyButton(".") { commitText(".") },
            LinearLayout.LayoutParams(0, dp(46), 0.8f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        row.addView(keyButton("↵") { sendEnter() },
            LinearLayout.LayoutParams(0, dp(46), 1.2f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(50)))
    }

    private fun keyButton(label: String, action: (() -> Unit)? = null): Button =
        Button(this).apply {
            text = label
            textSize = if (label.length > 1) 11f else 16f
            isAllCaps = false
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(2), 0, dp(2), 0)
            setOnClickListener {
                when {
                    action != null -> action()
                    label == "⇧" -> {
                        shifted = !shifted
                        statusView.text = if (shifted) "Shift on" else "DeskPad keyboard"
                    }
                    label == "⌫" -> backspace()
                    else -> {
                        commitText(if (shifted) label.uppercase() else label)
                        shifted = false
                        statusView.text = "DeskPad keyboard"
                    }
                }
            }
        }

    private fun sendEnter() {
        val info = currentInputEditorInfo
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val connection = currentInputConnection ?: return
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            connection.performEditorAction(action)
        } else {
            sendKey(KeyEvent.KEYCODE_ENTER)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        setActive(true)
        if (::statusView.isInitialized) statusView.text = "Editor connected"
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        setActive(true)
        if (::statusView.isInitialized) statusView.text = "Editor connected · ready"
    }

    override fun onFinishInput() {
        super.onFinishInput()
        setActive(false)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        setActive(false)
    }

    private fun setActive(value: Boolean) {
        active = value
        onActiveChanged?.invoke(value)
        if (::statusView.isInitialized && !value) statusView.text = "Waiting for editor"
    }

    val isActive: Boolean get() = active

    fun commit(text: CharSequence): Boolean =
        currentInputConnection?.commitText(text, 1) == true

    fun backspace(): Boolean =
        currentInputConnection?.deleteSurroundingText(1, 0) == true

    fun sendKey(keyCode: Int, metaState: Int = 0): Boolean {
        val ic = currentInputConnection ?: return false
        val now = SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, metaState))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, metaState))
        return true
    }

    private fun commitText(text: String) {
        if (!commit(text)) {
            if (::statusView.isInitialized) statusView.text = "No active text editor"
        }
    }

    companion object {
        @Volatile
        var instance: DeskPadIme? = null
            private set

        @Volatile
        var onActiveChanged: ((Boolean) -> Unit)? = null
    }
}
