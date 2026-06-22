package com.katib.app.ime

import android.content.res.Configuration
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.katib.app.KatibApplication
import com.katib.app.data.FreeTier
import com.katib.app.data.Premium
import com.katib.app.data.WritingMode
import com.katib.app.net.CorrectResponse
import com.katib.app.net.KatibApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * The Katib system-wide keyboard. Renders an Arabic layout plus an assistant
 * toolbar (mode toggle + suggestion/correction chips), and—after a typing
 * pause—asks the proxy for corrections and suggestions which the user can
 * apply by tapping a chip.
 *
 * Note on the spec: iOS-style inline underlining of errors inside other apps'
 * text is not possible from an Android IME (we cannot style text we did not
 * compose). The Android-equivalent UX is the chip bar above the keys, which is
 * also how Grammarly's Android keyboard works.
 */
class KatibInputMethodService : InputMethodService() {

    private val app get() = application as KatibApplication
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    private var mode: WritingMode = WritingMode.MSA
    private var isPremium: Boolean = false

    private var usingSymbols = false
    private lateinit var root: LinearLayout
    private lateinit var keysContainer: LinearLayout
    private lateinit var chipsContainer: LinearLayout
    private lateinit var modeButton: Button

    private val analyze = Runnable { runAnalysis() }

    private val isNight: Boolean
        get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    override fun onCreate() {
        super.onCreate()
        // Keep cached copies of preferences so view callbacks stay synchronous.
        app.prefs.mode.onEach { mode = it }.launchIn(scope)
        app.prefs.isPremium.onEach { isPremium = it }.launchIn(scope)
    }

    override fun onCreateInputView(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(if (isNight) 0xFF111111.toInt() else 0xFFD1D5DB.toInt())
            setPadding(0, dp(4), 0, dp(6))
        }
        root.addView(buildToolbar())
        keysContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(keysContainer)
        renderKeys()
        return root
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        clearChips()
    }

    override fun onDestroy() {
        scope.cancel()
        handler.removeCallbacks(analyze)
        super.onDestroy()
    }

    // ---------- toolbar ----------

    private fun buildToolbar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH, dp(46))
            setPadding(dp(6), 0, dp(6), 0)
        }

        modeButton = Button(this).apply {
            text = mode.arabicLabel
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(0xFF0A7B5E.toInt())
            setOnClickListener { toggleMode() }
            layoutParams = LinearLayout.LayoutParams(WRAP, dp(38))
        }
        bar.addView(modeButton)

        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f)
        }
        chipsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), 0, dp(6), 0)
        }
        scroll.addView(chipsContainer)
        bar.addView(scroll)

        val globe = Button(this).apply {
            text = "🌐"
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(38))
        }
        bar.addView(globe)
        return bar
    }

    private fun toggleMode() {
        val target = if (mode == WritingMode.MSA) WritingMode.GULF else WritingMode.MSA
        if (target == WritingMode.GULF && !isPremium) {
            showInfoChip("وضع الخليجي للمشتركين — ترقَّ لبريميوم")
            return
        }
        mode = target
        modeButton.text = mode.arabicLabel
        scope.launch { app.prefs.setMode(mode) }
    }

    // ---------- keys ----------

    private fun renderKeys() {
        keysContainer.removeAllViews()
        val layers = if (usingSymbols) ArabicKeyboardLayout.symbolLayer else ArabicKeyboardLayout.letterLayer
        val rowHeight = dp(52)
        for (rowKeys in layers) {
            val rowView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(MATCH, rowHeight)
            }
            for (key in rowKeys) {
                rowView.addView(makeKeyView(key, rowHeight))
            }
            keysContainer.addView(rowView)
        }
    }

    private fun makeKeyView(key: Key, rowHeight: Int): View {
        return Button(this).apply {
            text = key.label
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (key.type == KeyType.CHAR) 18f else 15f)
            setTextColor(if (isNight) Color.WHITE else 0xFF1C1C1E.toInt())
            setBackgroundColor(keyColor(key))
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, key.weight)
            lp.setMargins(dp(2), dp(3), dp(2), dp(3))
            layoutParams = lp
            setOnClickListener { onKey(key) }
        }
    }

    private fun keyColor(key: Key): Int = when (key.type) {
        KeyType.CHAR -> if (isNight) 0xFF2C2C2E.toInt() else Color.WHITE
        KeyType.SPACE -> if (isNight) 0xFF2C2C2E.toInt() else Color.WHITE
        else -> if (isNight) 0xFF3A3A3C.toInt() else 0xFFB7BCC2.toInt()
    }

    private fun onKey(key: Key) {
        val ic = currentInputConnection ?: return
        when (key.type) {
            KeyType.CHAR, KeyType.SPACE -> {
                ic.commitText(key.output, 1)
                scheduleAnalysis()
            }
            KeyType.BACKSPACE -> {
                val selected = ic.getSelectedText(0)
                if (selected.isNullOrEmpty()) ic.deleteSurroundingText(1, 0) else ic.commitText("", 1)
                scheduleAnalysis()
            }
            KeyType.ENTER -> {
                if (!sendDefaultEditorAction(true)) ic.commitText("\n", 1)
            }
            KeyType.SYMBOLS -> { usingSymbols = true; renderKeys() }
            KeyType.ABC -> { usingSymbols = false; renderKeys() }
            KeyType.MODE -> toggleMode()
        }
    }

    // ---------- assistant ----------

    private fun scheduleAnalysis() {
        handler.removeCallbacks(analyze)
        handler.postDelayed(analyze, 400) // debounce per spec
    }

    private fun runAnalysis() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(160, 0)?.toString().orEmpty()
        val after = ic.getTextAfterCursor(60, 0)?.toString().orEmpty()
        val text = (before + after).trim()
        if (text.length < 2) { clearChips(); return }

        scope.launch {
            // Free-tier daily gate.
            val remaining = app.prefs.correctionsRemainingToday(isPremium)
            if (remaining != null && remaining <= 0) {
                clearChips()
                showInfoChip("بلغت الحد اليومي المجاني — ترقَّ لبريميوم")
                return@launch
            }

            val requestMode = if (isPremium) mode.wire else WritingMode.MSA.wire
            when (val res = app.api.correct(text, requestMode, "correct")) {
                is KatibApiClient.Result.Ok -> {
                    if (res.response.corrections.isNotEmpty()) {
                        if (!isPremium) app.prefs.recordCorrectionShown()
                        res.response.corrections.firstOrNull()?.let { app.prefs.recordErrorType(it.type) }
                        showCorrectionChips(res.response)
                    } else {
                        // No errors — offer synonym suggestions for the last word.
                        fetchSuggestions(text, requestMode)
                    }
                }
                is KatibApiClient.Result.Error -> clearChips()
            }
        }
    }

    private suspend fun fetchSuggestions(text: String, requestMode: String) {
        when (val res = app.api.correct(text, requestMode, "suggest")) {
            is KatibApiClient.Result.Ok -> showSuggestionChips(res.response)
            is KatibApiClient.Result.Error -> clearChips()
        }
    }

    private fun showCorrectionChips(res: CorrectResponse) {
        clearChips()
        res.corrections.take(3).forEach { c ->
            addChip("${c.original} ← ${c.corrected}", 0xFFE8730C.toInt()) {
                applyReplacement(c.original, c.corrected)
                clearChips()
            }
        }
    }

    private fun showSuggestionChips(res: CorrectResponse) {
        clearChips()
        val maxChips = if (isPremium) Premium.SUGGESTION_CHIPS else FreeTier.SUGGESTION_CHIPS
        val group = res.suggestions.firstOrNull() ?: return
        group.options.take(maxChips).forEach { opt ->
            addChip("${opt.text} · ${opt.register}", 0xFF0A7B5E.toInt()) {
                applyReplacement(group.target, opt.text)
                scope.launch { app.prefs.recordSuggestionAccepted() }
                clearChips()
            }
        }
    }

    /** Replace the most recent occurrence of [original] before the cursor with [replacement]. */
    private fun applyReplacement(original: String, replacement: String) {
        if (original.isEmpty()) return
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(200, 0)?.toString() ?: return
        val idx = before.lastIndexOf(original)
        if (idx < 0) return
        val tail = before.substring(idx + original.length)
        ic.beginBatchEdit()
        ic.deleteSurroundingText(before.length - idx, 0)
        ic.commitText(replacement + tail, 1)
        ic.endBatchEdit()
    }

    // ---------- chips ----------

    private fun clearChips() {
        if (this::chipsContainer.isInitialized) chipsContainer.removeAllViews()
    }

    private fun showInfoChip(label: String) {
        clearChips()
        val tv = TextView(this).apply {
            text = label
            setTextColor(if (isNight) Color.LTGRAY else Color.DKGRAY)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        chipsContainer.addView(tv)
    }

    private fun addChip(label: String, color: Int, onClick: () -> Unit) {
        val chip = Button(this).apply {
            text = label
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.WHITE)
            setBackgroundColor(color)
            setPadding(dp(10), dp(2), dp(10), dp(2))
            val lp = LinearLayout.LayoutParams(WRAP, dp(34))
            lp.setMargins(dp(4), 0, dp(4), 0)
            layoutParams = lp
            setOnClickListener { onClick() }
        }
        chipsContainer.addView(chip)
    }

    // ---------- helpers ----------

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    companion object {
        private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
