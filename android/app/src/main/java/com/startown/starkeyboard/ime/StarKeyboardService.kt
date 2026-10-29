package com.startown.starkeyboard.ime

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.startown.starkeyboard.MainActivity
import com.startown.starkeyboard.core.Candidate
import com.startown.starkeyboard.core.ClipboardStore
import com.startown.starkeyboard.core.EmoticonStore
import com.startown.starkeyboard.core.HangulComposer
import com.startown.starkeyboard.core.Romaji
import com.startown.starkeyboard.core.Spelling
import com.startown.starkeyboard.core.UserPhraseModel
import com.startown.starkeyboard.core.ZhuyinComposer
import com.startown.starkeyboard.core.ZhuyinEngine
import java.io.File
import java.util.Locale

class StarKeyboardService : InputMethodService(), SpellCheckerSession.SpellCheckerSessionListener {
    private enum class SuggestionSource { ASSOCIATION, HANDWRITING, SPELLING }
    private enum class EditorPurpose { EMOTICON, TRANSLATE }

    private lateinit var settings: KeyboardSettings
    private lateinit var clipStore: ClipboardStore
    private lateinit var emoStore: EmoticonStore
    private var engine: ZhuyinEngine? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // 狀態
    private var mode = InputMode.ZHUYIN
    private var layer = KeyboardLayer.LETTERS
    private var shift = ShiftState.OFF
    private var lastShiftTap = 0L
    private var lastSpaceTime = 0L
    private val composer = ZhuyinComposer()
    private var kanaBuffer = ""
    private val hangul = HangulComposer()
    private var hangulComposing = false
    private var candidates: List<Candidate> = emptyList()
    private var suggestions: List<String> = emptyList()
    private var suggestionSource = SuggestionSource.ASSOCIATION
    private var panel: PanelKind? = null
    private var editorText: String? = null
    private var editorPurpose = EditorPurpose.EMOTICON
    private var editorReturnPanel: PanelKind? = null
    private var translation: Pair<String, String>? = null
    private var translating = false
    private var lastAutocorrect: Pair<String, String>? = null
    private var styleKey = ""

    // 英文拼字檢查（系統的拼字檢查服務）
    private var spellSession: SpellCheckerSession? = null
    private val spellCache = HashMap<String, List<String>>()   // 打錯的字 → 建議；拼對的字 → 空清單
    private var spellPending: String? = null

    // 畫面
    private var root: FrameLayout? = null
    private lateinit var bgImage: ImageView
    private lateinit var tint: View
    private lateinit var topBar: TopBar
    private lateinit var content: FrameLayout
    private lateinit var keyboard: KeyboardView
    private var panelView: View? = null
    private var candidatePanel: CandidatePanel? = null
    private var handwriting: HandwritingPanel? = null
    private lateinit var toastView: TextView
    private val ink by lazy { InkRecognizer { toast(it) } }

    private val style get() = settings.style
    private val isComposing get() = composer.isComposing || kanaBuffer.isNotEmpty()
    private val dp get() = resources.displayMetrics.density

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener { captureClipboard() }

    // ===== 生命週期 =====

    override fun onCreate() {
        super.onCreate()
        settings = KeyboardSettings(this)
        clipStore = ClipboardStore(File(filesDir, "clipboard.json"), settings.clipLimit)
        emoStore = EmoticonStore(filesDir)
        mode = settings.lastMode.takeIf { it in settings.enabledModes } ?: settings.enabledModes.first()
        Thread {
            val source = SQLitePhraseSource.open(this)
            mainHandler.post {
                if (source != null) {
                    engine = ZhuyinEngine(source, UserPhraseModel(File(filesDir, "learned.txt"))).also { it.fuzzy = fuzzyOptions() }
                }
            }
        }.start()
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).addPrimaryClipChangedListener(clipListener)
        runCatching {
            val tsm = getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
            spellSession = tsm.newSpellCheckerSession(null, Locale.ENGLISH, this, true)
        }
    }

    override fun onDestroy() {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).removePrimaryClipChangedListener(clipListener)
        spellSession?.close()
        ink.close()
        super.onDestroy()
    }

    private fun fuzzyOptions() = ZhuyinEngine.FuzzyOptions(settings.fuzzyKeys, settings.fuzzyTones, settings.fuzzyPhonetic)

    override fun onEvaluateFullscreenMode() = false

    override fun onCreateInputView(): View {
        val r = FrameLayout(this)
        bgImage = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        tint = View(this)
        r.addView(bgImage, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        r.addView(tint, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        topBar = TopBar(this, style)
        content = FrameLayout(this)
        keyboard = KeyboardView(this, style)
        content.addView(keyboard, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        column.addView(topBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (46 * dp).toInt()))
        column.addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, contentHeight()))
        r.addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        toastView = TextView(this).apply {
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding((14 * dp).toInt(), (8 * dp).toInt(), (14 * dp).toInt(), (8 * dp).toInt())
            alpha = 0f
            maxLines = 2
        }
        r.addView(toastView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL or Gravity.TOP).apply {
            topMargin = (54 * dp).toInt()
        })
        root = r
        wire()
        applyStyle()
        rebuildKeys()
        refreshTopBar()
        return r
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        emoStore.load()
        clipStore.load()
        if (mode !in settings.enabledModes) mode = settings.enabledModes.first()
        resetComposition()
        applyStyle()
        captureClipboard()
        checkForUpdate()
        rebuildKeys()
        updateAutoShift()
        refreshTopBar()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        resetComposition()
        super.onFinishInputView(finishingInput)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (root != null) rebuildKeys()
    }

    private fun resetComposition() {
        composer.clear()
        kanaBuffer = ""
        hangul.reset()
        hangulComposing = false
        candidates = emptyList()
        suggestions = emptyList()
        currentInputConnection?.finishComposingText()
    }

    private fun wire() {
        keyboard.onKey = { handle(it) }
        keyboard.onLongPress = { showImePicker() }
        keyboard.onCursorMove = { steps -> if (!isComposing) moveCursor(steps) }
        topBar.onToolbar = { toolbarTapped(it) }
        topBar.onCandidate = { selectCandidate(it, true) }
        topBar.onTrailing = { trailing ->
            when (trailing) {
                Trailing.EXPAND -> toggleCandidatePanel()
                Trailing.CLOSE -> {
                    if (translation != null) endEditor() else {
                        if (suggestionSource == SuggestionSource.HANDWRITING) handwriting?.clear()
                        clearSuggestions()
                    }
                }
            }
        }
        topBar.onEditor = { confirm -> if (confirm) confirmEditor() else endEditor() }
        topBar.onEditorExtra = { editorExtraTapped() }
    }

    private fun showImePicker() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    private fun moveCursor(steps: Int) {
        val ic = currentInputConnection ?: return
        finishHangul()
        val code = if (steps > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        repeat(kotlin.math.abs(steps)) {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        }
    }

    // ===== 按鍵 =====

    private fun handle(spec: KeySpec) {
        feedback()
        val pending = lastAutocorrect
        lastAutocorrect = null
        val action = spec.action
        if (action == KeyAction.Backspace && pending != null && revertAutocorrect(pending)) return
        if (action !is KeyAction.Jamo && action != KeyAction.Backspace && action != KeyAction.Shift) finishHangul()
        when (action) {
            is KeyAction.Text -> typeText(action.s)
            is KeyAction.Zhuyin -> typeZhuyin(action.c)
            is KeyAction.Jamo -> typeJamo(action.c)
            is KeyAction.Romaji -> typeRomaji(action.c)
            is KeyAction.Layer -> { layer = action.layer; rebuildKeys() }
            is KeyAction.Panel -> openPanel(action.kind)
            KeyAction.Backspace -> backspace()
            KeyAction.Space -> space()
            KeyAction.Return -> returnKey()
            KeyAction.Shift -> toggleShift()
            KeyAction.NextMode -> switchMode()
            KeyAction.ClearCanvas -> { handwriting?.clear(); clearSuggestions() }
        }
    }

    private fun typeText(s: String) {
        if (isComposing) commitAll()
        clearSuggestions()
        if (mode == InputMode.ENGLISH && s in listOf(".", ",", "!", "?", ";", ":")) applyAutocorrect()
        output(s)
        if (shift == ShiftState.ON) {
            shift = ShiftState.OFF
            rebuildKeys()
        }
        updateSpelling()
    }

    private fun typeZhuyin(c: Char) {
        suggestions = emptyList()
        if (!composer.input(c)) {
            output(c.toString())
            refreshTopBar()
            return
        }
        if (composer.syllables.size >= 20) {
            commitAll()
            return
        }
        updateCandidates()
    }

    private fun typeRomaji(c: Char) {
        suggestions = emptyList()
        kanaBuffer += c
        if (kanaBuffer.length >= 60) {
            commitAll()
            return
        }
        updateCandidates()
    }

    private fun typeJamo(c: Char) {
        clearSuggestions()
        val out = hangul.input(c)
        if (editorText != null) {
            // 編輯框裡：只把拼完的字放進去，正在拼的顯示在後面
            editorText = (editorText ?: "") + out.commit
            refreshTopBar()
        } else {
            val ic = currentInputConnection
            if (out.commit.isNotEmpty()) ic?.commitText(out.commit, 1)
            if (out.composing.isNotEmpty()) ic?.setComposingText(out.composing, 1)
        }
        hangulComposing = out.composing.isNotEmpty()
        if (shift == ShiftState.ON) {
            shift = ShiftState.OFF
            rebuildKeys()
        }
    }

    private fun finishHangul() {
        if (!hangulComposing) return
        if (editorText != null) {
            editorText = (editorText ?: "") + hangul.composing
            refreshTopBar()
        } else {
            currentInputConnection?.finishComposingText()
        }
        hangul.reset()
        hangulComposing = false
    }

    private fun backspace() {
        if (kanaBuffer.isNotEmpty()) {
            kanaBuffer = kanaBuffer.dropLast(1)
            updateCandidates()
            return
        }
        if (composer.isComposing) {
            composer.backspace()
            updateCandidates()
            return
        }
        if (hangulComposing && hangul.backspace()) {
            val c = hangul.composing
            if (editorText == null) currentInputConnection?.setComposingText(c, 1)
            hangulComposing = c.isNotEmpty()
            if (!hangulComposing) currentInputConnection?.finishComposingText()
            return
        }
        finishHangul()
        clearSuggestions()
        deleteOutput()
        updateAutoShift()
        updateSpelling()
    }

    private fun space() {
        if (mode == InputMode.ZHUYIN && layer == KeyboardLayer.LETTERS && composer.hasPendingSyllable) {
            if (composer.confirmSyllable()) updateCandidates()
            return
        }
        if (isComposing) {
            if (candidates.isEmpty()) commitAll() else selectCandidate(0, false)
            return
        }
        if (suggestionSource == SuggestionSource.HANDWRITING && suggestions.isNotEmpty()) {
            selectCandidate(0, false)
            return
        }
        clearSuggestions()
        val now = System.currentTimeMillis()
        // 英文：快速按兩下空白變成句號
        if (mode == InputMode.ENGLISH && editorText == null && now - lastSpaceTime < 400) {
            val before = currentInputConnection?.getTextBeforeCursor(2, 0)?.toString() ?: ""
            if (before.length == 2 && before[1] == ' ' && before[0].isLetterOrDigit()) {
                deleteOutput()
                output(". ")
                lastSpaceTime = 0
                updateAutoShift()
                return
            }
        }
        applyAutocorrect()
        lastSpaceTime = now
        output(" ")
        updateAutoShift()
    }

    private fun returnKey() {
        if (isComposing) {
            commitAll()
            return
        }
        if (editorText != null) {
            confirmEditor()
            return
        }
        clearSuggestions()
        val info = currentInputEditorInfo
        val action = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val noEnterAction = ((info?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            currentInputConnection?.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
        updateAutoShift()
    }

    private fun toggleShift() {
        val now = System.currentTimeMillis()
        val quick = now - lastShiftTap < 350
        shift = when (shift) {
            ShiftState.OFF -> ShiftState.ON
            ShiftState.ON -> if (quick) ShiftState.LOCKED else ShiftState.OFF
            ShiftState.LOCKED -> ShiftState.OFF
        }
        lastShiftTap = now
        rebuildKeys()
    }

    private fun updateAutoShift() {
        if (mode != InputMode.ENGLISH || layer != KeyboardLayer.LETTERS || shift == ShiftState.LOCKED || editorText != null) return
        val info = currentInputEditorInfo ?: return
        val caps = currentInputConnection?.getCursorCapsMode(info.inputType) ?: 0
        val want = if (caps != 0) ShiftState.ON else ShiftState.OFF
        if (want != shift) {
            shift = want
            rebuildKeys()
        }
    }

    private fun switchMode() {
        commitAll()
        finishHangul()
        clearSuggestions()
        val modes = settings.enabledModes
        val i = modes.indexOf(mode)
        mode = if (i >= 0) modes[(i + 1) % modes.size] else modes.first()
        settings.lastMode = mode
        layer = KeyboardLayer.LETTERS
        shift = ShiftState.OFF
        rebuildKeys()
        updateAutoShift()
    }

    private val nextModeTitle: String
        get() {
            val modes = settings.enabledModes
            if (modes.size <= 1) return mode.short
            val i = maxOf(0, modes.indexOf(mode))
            return modes[(i + 1) % modes.size].short
        }

    // ===== 輸出（用編輯框時打到編輯框，平常打到 App） =====

    private fun output(s: String) {
        if (s.isEmpty()) return
        if (editorText != null && translation == null) {
            editorText = editorText + s
            refreshTopBar()
            return
        }
        currentInputConnection?.commitText(s, 1)
    }

    private fun deleteOutput() {
        val t = editorText
        if (t != null && translation == null) {
            if (t.isNotEmpty()) editorText = t.substring(0, t.offsetByCodePoints(t.length, -1))
            refreshTopBar()
            return
        }
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        when {
            !selected.isNullOrEmpty() -> ic.commitText("", 1)
            ic.getTextBeforeCursor(1, 0).isNullOrEmpty() -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            else -> ic.deleteSurroundingTextInCodePoints(1, 0)
        }
    }

    /** 注音、日文組字時，把正在打的字顯示在輸入框裡（底下有底線） */
    private fun syncComposing() {
        if (editorText != null) return
        val ic = currentInputConnection ?: return
        if (isComposing) ic.setComposingText(compositionText, 1) else ic.finishComposingText()
    }

    // ===== 選字（注音、日文） =====

    private fun updateCandidates() {
        candidates = when {
            kanaBuffer.isNotEmpty() -> {
                val hira = Romaji.finalize(kanaBuffer)
                listOf(hira, Romaji.katakana(hira), kanaBuffer).distinct().map { Candidate(it, 0, Candidate.Kind.PHRASE) }
            }
            composer.syllables.isEmpty() -> emptyList()
            else -> engine?.candidates(composer.readings)
                ?: listOf(Candidate(composer.readings.joinToString(""), composer.syllables.size, Candidate.Kind.LITERAL))
        }
        if (!isComposing) {
            currentInputConnection?.setComposingText("", 1)
            closeCandidatePanel()
        }
        syncComposing()
        candidatePanel?.setCandidates(candidates.map { it.text })
        refreshTopBar()
        refreshReturnTitle()
    }

    private val compositionText: String
        get() {
            if (kanaBuffer.isNotEmpty()) {
                val (kana, rest) = Romaji.convert(kanaBuffer)
                return kana + rest
            }
            val head = if (composer.syllables.isEmpty()) "" else (candidates.firstOrNull()?.text ?: composer.readings.joinToString(""))
            return head + composer.current.reading
        }

    private fun selectCandidate(i: Int, learn: Boolean) {
        translation?.let { t ->
            if (i == 0) {
                editorText = null
                translation = null
                currentInputConnection?.commitText(t.second, 1)
                refreshTopBar()
                rebuildKeys()
            } else {
                translation = null
                refreshTopBar()
            }
            return
        }
        if (kanaBuffer.isNotEmpty()) {
            val text = candidates.getOrNull(i)?.text ?: return
            kanaBuffer = ""
            candidates = emptyList()
            closeCandidatePanel()
            output(text)
            refreshTopBar()
            refreshReturnTitle()
            return
        }
        if (composer.isComposing) {
            val c = candidates.getOrNull(i) ?: return
            if (learn && c.kind != Candidate.Kind.LITERAL) engine?.learn(composer.readings.take(c.span), c.text)
            output(c.text)
            composer.removeFirst(c.span)
            if (composer.isComposing) {
                updateCandidates()
            } else {
                candidates = emptyList()
                closeCandidatePanel()
                showAssociations(c.text)
                refreshReturnTitle()
            }
            return
        }
        if (suggestionSource == SuggestionSource.SPELLING) {
            val choice = suggestions.getOrNull(i) ?: return
            val word = currentWord
            val ic = currentInputConnection
            if (choice != word) {
                ic?.deleteSurroundingText(word.length, 0)
                ic?.commitText(choice, 1)
            }
            ic?.commitText(" ", 1)
            clearSuggestions()
            updateAutoShift()
            return
        }
        val s = suggestions.getOrNull(i) ?: return
        output(s)
        if (suggestionSource == SuggestionSource.HANDWRITING) handwriting?.clear()
        showAssociations(s)
    }

    private fun commitAll() {
        if (!isComposing) return
        val text: String
        if (kanaBuffer.isNotEmpty()) {
            text = Romaji.finalize(kanaBuffer)
            kanaBuffer = ""
        } else {
            val head = if (composer.syllables.isEmpty()) "" else
                engine?.bestSegments(composer.readings)?.joinToString("") { it.text } ?: composer.readings.joinToString("")
            text = head + composer.current.reading
            composer.clear()
        }
        candidates = emptyList()
        closeCandidatePanel()
        if (editorText != null) output(text) else currentInputConnection?.commitText(text, 1)
        refreshTopBar()
        refreshReturnTitle()
    }

    private fun showAssociations(text: String) {
        suggestions = if (settings.associations && editorText == null && (mode == InputMode.ZHUYIN || mode == InputMode.HANDWRITING)) {
            engine?.associations(text) ?: emptyList()
        } else emptyList()
        suggestionSource = SuggestionSource.ASSOCIATION
        refreshTopBar()
    }

    private fun clearSuggestions() {
        if (suggestions.isEmpty()) return
        suggestions = emptyList()
        refreshTopBar()
    }

    private fun refreshTopBar() {
        if (root == null) return
        val tr = translation
        val ed = editorText
        when {
            tr != null -> topBar.showCandidates("點翻譯結果送出 ・ 原文：${tr.first}", listOf(tr.second, "✏️ 修改原文"), Trailing.CLOSE)
            isComposing -> {
                val prefix = if (ed != null) (if (editorPurpose == EditorPurpose.TRANSLATE) "翻譯：" else "表符：") + ed + " ｜ " else ""
                topBar.showCandidates(if (ed != null) prefix + compositionText else "", candidates.map { it.text }, Trailing.EXPAND)
            }
            ed != null && (suggestions.isEmpty() || suggestionSource == SuggestionSource.ASSOCIATION) -> {
                val pending = if (hangulComposing) hangul.composing else ""
                if (editorPurpose == EditorPurpose.EMOTICON) {
                    topBar.showEditor("新增表符：", ed + pending, "(•ᴗ•)", "儲存")
                } else {
                    val name = Translator.targets.firstOrNull { it.code == settings.translateTarget }?.name ?: "英文"
                    topBar.showEditor(if (translating) "翻譯中…" else "翻譯：", ed + pending, "→$name", "翻譯")
                }
            }
            suggestions.isNotEmpty() -> topBar.showCandidates("", suggestions, Trailing.CLOSE)
            else -> topBar.showToolbar()
        }
    }

    private fun toggleCandidatePanel() {
        if (candidatePanel != null) {
            closeCandidatePanel()
            return
        }
        val p = CandidatePanel(this, style, { selectCandidate(it, true) }, { closeCandidatePanel() }, { backspace() })
        p.setCandidates(candidates.map { it.text })
        content.addView(p, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        candidatePanel = p
    }

    private fun closeCandidatePanel() {
        candidatePanel?.let { content.removeView(it) }
        candidatePanel = null
    }

    // ===== 英文自動修正 =====

    /** 游標前面正在打的英文字 */
    private val currentWord: String
        get() {
            if (editorText != null) return ""
            val before = currentInputConnection?.getTextBeforeCursor(40, 0)?.toString() ?: return ""
            return before.takeLastWhile { (it.isLetter() && it.code < 128) || it == '\'' }
        }

    private fun updateSpelling() {
        if (mode != InputMode.ENGLISH || !settings.autocorrect) return
        val word = currentWord
        if (word.length < 2) {
            if (suggestionSource == SuggestionSource.SPELLING) clearSuggestions()
            return
        }
        val cached = spellCache[word]
        if (cached != null) {
            showSpelling(word, cached)
        } else {
            spellPending = word
            spellSession?.getSentenceSuggestions(arrayOf(TextInfo(word)), 5)
        }
    }

    private fun showSpelling(word: String, fixes: List<String>) {
        if (word != currentWord) return
        val options = fixes.map { Spelling.matchCase(it, word) }.filter { it != word }.distinct()
        if (options.isEmpty()) {
            if (suggestionSource == SuggestionSource.SPELLING) clearSuggestions()
            return
        }
        suggestions = listOf(word) + options.take(5)
        suggestionSource = SuggestionSource.SPELLING
        refreshTopBar()
    }

    override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
        val word = spellPending ?: return
        val fixes = ArrayList<String>()
        var typo = false
        results?.forEach { s ->
            for (i in 0 until s.suggestionsCount) {
                val info = s.getSuggestionsInfoAt(i) ?: continue
                val attrs = info.suggestionsAttributes
                if (attrs and (SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO or SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS) != 0 &&
                    attrs and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY == 0) {
                    typo = true
                }
                for (j in 0 until info.suggestionsCount) info.getSuggestionAt(j)?.let { fixes.add(it) }
            }
        }
        val list = if (typo) fixes else emptyList()
        spellCache[word] = list
        mainHandler.post { showSpelling(word, list) }
    }

    override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {}

    private fun applyAutocorrect(): Boolean {
        if (mode != InputMode.ENGLISH || editorText != null || !settings.autocorrect) return false
        val word = currentWord
        if (word.length < 2 || word.uppercase() == word) return false
        val fix = spellCache[word]?.firstOrNull() ?: return false
        if (!Spelling.isCloseEnough(word, fix)) return false
        val fixed = Spelling.matchCase(fix, word)
        val ic = currentInputConnection ?: return false
        ic.deleteSurroundingText(word.length, 0)
        ic.commitText(fixed, 1)
        lastAutocorrect = word to fixed
        return true
    }

    private fun revertAutocorrect(ac: Pair<String, String>): Boolean {
        val ic = currentInputConnection ?: return false
        val (original, corrected) = ac
        val before = ic.getTextBeforeCursor(corrected.length + 1, 0)?.toString() ?: return false
        if (before.length != corrected.length + 1 || !before.startsWith(corrected) || before.last().isLetter()) return false
        val last = before.last()
        ic.deleteSurroundingText(corrected.length + 1, 0)
        ic.commitText(if (last == ' ') original else original + last, 1)
        spellCache[original] = emptyList()
        clearSuggestions()
        toast("已改回「$original」，之後不會再改它")
        return true
    }

    // ===== 工具列與面板 =====

    private fun toolbarTapped(item: ToolbarItem) {
        feedback()
        finishHangul()
        when (item) {
            ToolbarItem.CLIPBOARD -> togglePanel(PanelKind.CLIPBOARD)
            ToolbarItem.EMOJI -> togglePanel(PanelKind.EMOJI)
            ToolbarItem.KAOMOJI -> togglePanel(PanelKind.KAOMOJI)
            ToolbarItem.SYMBOLS -> togglePanel(PanelKind.SYMBOLS)
            ToolbarItem.SETTINGS -> togglePanel(PanelKind.SETTINGS)
            ToolbarItem.TRANSLATE -> beginEditor(EditorPurpose.TRANSLATE)
            ToolbarItem.HANDWRITING -> {
                closePanel()
                if (mode == InputMode.HANDWRITING) {
                    switchMode()
                } else {
                    if (InputMode.HANDWRITING !in settings.enabledModes) settings.enabledModes = settings.enabledModes + InputMode.HANDWRITING
                    commitAll()
                    mode = InputMode.HANDWRITING
                    settings.lastMode = mode
                    layer = KeyboardLayer.LETTERS
                    rebuildKeys()
                }
            }
            ToolbarItem.DISMISS -> {
                commitAll()
                requestHideSelf(0)
            }
        }
    }

    private fun togglePanel(kind: PanelKind) {
        if (panel == kind) closePanel() else openPanel(kind)
    }

    private fun openPanel(kind: PanelKind) {
        commitAll()
        finishHangul()
        closePanel()
        panel = kind
        val v: View = when (kind) {
            PanelKind.EMOJI, PanelKind.KAOMOJI, PanelKind.SYMBOLS -> EmojiPanel(this, kind, emoStore, style,
                onInsert = { feedback(); output(it) },
                onSticker = { sendSticker(it) },
                onAdd = { addEmoticon(it) },
                onBack = { closePanel() },
                onBackspace = { deleteOutput() })
            PanelKind.CLIPBOARD -> {
                captureClipboard()
                ClipboardPanel(this, clipStore, style,
                    onInsert = { feedback(); output(it) },
                    onAddCurrent = { addCurrentClipboard() },
                    onBack = { closePanel() },
                    onBackspace = { deleteOutput() })
            }
            PanelKind.SETTINGS -> SettingsPanel(this, settings, style,
                onChange = { settingsChanged() },
                onOpenApp = { openApp() },
                onResetLearning = { engine?.resetLearning(); toast("已清除選字學習紀錄") },
                onBack = { closePanel() })
        }
        content.addView(v, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        panelView = v
        keyboard.visibility = View.GONE
        handwriting?.visibility = View.GONE
    }

    private fun closePanel() {
        panelView?.let { content.removeView(it) }
        panelView = null
        panel = null
        val showHandwriting = mode == InputMode.HANDWRITING && layer == KeyboardLayer.LETTERS
        keyboard.visibility = if (showHandwriting) View.GONE else View.VISIBLE
        handwriting?.visibility = if (showHandwriting) View.VISIBLE else View.GONE
    }

    private fun settingsChanged() {
        clipStore.limit = settings.clipLimit
        engine?.fuzzy = fuzzyOptions()
        if (mode !in settings.enabledModes) {
            mode = settings.enabledModes.first()
            settings.lastMode = mode
            layer = KeyboardLayer.LETTERS
        }
        val oldKey = styleKey
        applyStyle()
        rebuildKeys()
        // 換了主題或造型才重建設定面板（拖滑桿時不重建，不然滑桿會被拿掉）
        if (panel == PanelKind.SETTINGS && oldKey != styleKey) {
            openPanel(PanelKind.SETTINGS)
        }
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // ===== 剪貼簿 =====

    private fun captureClipboard() {
        runCatching {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip ?: return
            if (clip.itemCount == 0) return
            val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: return
            if (clipStore.add(text)) (panelView as? ClipboardPanel)?.reload()
        }
    }

    private fun addCurrentClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
        if (text.isNullOrBlank() || !clipStore.add(text)) {
            toast("剪貼簿裡沒有文字")
            return
        }
        (panelView as? ClipboardPanel)?.reload()
    }

    // ===== 編輯框（新增表符、翻譯） =====

    private fun beginEditor(purpose: EditorPurpose) {
        commitAll()
        finishHangul()
        suggestions = emptyList()
        editorReturnPanel = if (purpose == EditorPurpose.EMOTICON) panel else null
        closePanel()
        if (purpose == EditorPurpose.EMOTICON && mode == InputMode.HANDWRITING) {
            mode = settings.enabledModes.firstOrNull { it != InputMode.HANDWRITING } ?: InputMode.ZHUYIN
        }
        layer = KeyboardLayer.LETTERS
        editorPurpose = purpose
        editorText = ""
        translation = null
        rebuildKeys()
        refreshTopBar()
        toast(if (purpose == EditorPurpose.EMOTICON) "用鍵盤打字，或按 (•ᴗ•) 挑顏文字零件" else "先打要翻譯的話，按「→語言」換目標語言")
    }

    private fun editorExtraTapped() {
        if (editorPurpose == EditorPurpose.EMOTICON) {
            togglePanel(PanelKind.KAOMOJI)
        } else {
            val targets = Translator.targets
            val i = maxOf(0, targets.indexOfFirst { it.code == settings.translateTarget })
            settings.translateTarget = targets[(i + 1) % targets.size].code
            refreshTopBar()
        }
    }

    private fun confirmEditor() {
        commitAll()
        finishHangul()
        val text = editorText?.trim() ?: return
        if (editorPurpose == EditorPurpose.EMOTICON) {
            if (text.isNotEmpty()) {
                emoStore.addCustom(text)
                toast("已加入我的表符")
            }
            val back = editorReturnPanel
            endEditor()
            back?.let { openPanel(it) }
            return
        }
        if (text.isEmpty()) {
            toast("先打要翻譯的話")
            return
        }
        if (translating) return
        translating = true
        refreshTopBar()
        Translator.translate(text, settings.translateTarget, status = { toast(it) }) { result ->
            translating = false
            if (editorText == null) return@translate
            result.onSuccess { translation = text to it }.onFailure { toast(it.message ?: "翻譯失敗") }
            refreshTopBar()
            refreshReturnTitle()
        }
    }

    private fun endEditor() {
        composer.clear()
        kanaBuffer = ""
        hangul.reset()
        hangulComposing = false
        candidates = emptyList()
        editorText = null
        editorReturnPanel = null
        translation = null
        translating = false
        refreshTopBar()
        rebuildKeys()
    }

    // ===== 我的表符、貼圖 =====

    private fun addEmoticon(option: EmojiPanel.AddOption) {
        when (option) {
            EmojiPanel.AddOption.TYPE -> beginEditor(EditorPurpose.EMOTICON)
            EmojiPanel.AddOption.CLIPBOARD_TEXT -> {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
                if (text.isNullOrBlank()) {
                    toast("剪貼簿裡沒有文字")
                    return
                }
                emoStore.addCustom(text)
                (panelView as? EmojiPanel)?.reload()
                toast("已加入我的表符")
            }
            EmojiPanel.AddOption.PICK_IMAGE -> {
                startActivity(Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(MainActivity.EXTRA_PICK, MainActivity.PICK_STICKER))
            }
        }
    }

    /** 貼圖：App 支援的話直接送出圖片，不支援就複製到剪貼簿 */
    private fun sendSticker(name: String) {
        val file = emoStore.stickerFile(name) ?: return
        val uri = runCatching { FileProvider.getUriForFile(this, "$packageName.files", file) }.getOrNull() ?: return
        val info = currentInputEditorInfo
        val ic = currentInputConnection
        val types = if (info != null) EditorInfoCompat.getContentMimeTypes(info) else emptyArray()
        val supports = types.any { ClipDescription.compareMimeTypes("image/png", it) }
        if (supports && ic != null && info != null) {
            val content = InputContentInfoCompat(uri, ClipDescription("貼圖", arrayOf("image/png")), null)
            if (InputConnectionCompat.commitContent(ic, info, content, InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null)) return
        }
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newUri(contentResolver, "貼圖", uri))
        toast("這個 App 不能直接送圖，已複製貼圖，長按輸入框「貼上」")
    }

    // ===== 主題與版面 =====

    private fun applyStyle() {
        val r = root ?: return
        val s = style
        styleKey = s.theme.id + s.shape.name
        r.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(s.theme.top, s.theme.bottom))
        if (settings.hasBackground) {
            bgImage.setImageBitmap(BitmapFactory.decodeFile(settings.backgroundFile.path))
            bgImage.visibility = View.VISIBLE
            tint.setBackgroundColor(s.theme.top.withAlpha(settings.backgroundFade))
            tint.visibility = View.VISIBLE
        } else {
            bgImage.setImageDrawable(null)
            bgImage.visibility = View.GONE
            tint.visibility = View.GONE
        }
        topBar.style = s
        keyboard.style = s
        handwriting?.style = s
        toastView.background = roundRect(s.theme.accent.withAlpha(0.95f), 14 * dp)
        toastView.setTextColor(s.theme.accentText)
        refreshTopBar()
    }

    private var lastReturnTitle = ""

    private val returnTitle: String
        get() {
            if (isComposing) return "確認"
            if (translation != null) return "送出"
            if (editorText != null) return if (editorPurpose == EditorPurpose.TRANSLATE) "翻譯" else "儲存"
            val info = currentInputEditorInfo ?: return "換行"
            if (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return "換行"
            return when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
                EditorInfo.IME_ACTION_GO -> "前往"
                EditorInfo.IME_ACTION_SEARCH -> "搜尋"
                EditorInfo.IME_ACTION_SEND -> "傳送"
                EditorInfo.IME_ACTION_NEXT -> "下一項"
                EditorInfo.IME_ACTION_DONE -> "完成"
                else -> "換行"
            }
        }

    private val spaceTitle get() = style.theme.decoration.let { if (it.isEmpty()) mode.title else "$it ${mode.title}" }

    private fun refreshReturnTitle() {
        if (returnTitle != lastReturnTitle) rebuildKeys()
    }

    private val isLandscape get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private fun contentHeight(): Int {
        val fiveRows = mode == InputMode.ZHUYIN && layer == KeyboardLayer.LETTERS
        val base = if (isLandscape) (if (fiveRows) 196f else 164f) else (if (fiveRows) 252f else 216f)
        val scale = if (isLandscape) minOf(settings.heightScale, 1.1f) else settings.heightScale
        return (base * scale * dp).toInt()
    }

    private fun rebuildKeys() {
        if (root == null) return
        lastReturnTitle = returnTitle
        val ctx = KeyLayouts.Context(mode, layer, shift, returnTitle, spaceTitle, nextModeTitle)
        keyboard.setLayout(KeyLayouts.layout(ctx))
        val showHandwriting = mode == InputMode.HANDWRITING && layer == KeyboardLayer.LETTERS
        if (showHandwriting) {
            val hw = handwriting ?: HandwritingPanel(this, style, ink).also { hw ->
                hw.side.onKey = { handle(it) }
                hw.side.onLongPress = { showImePicker() }
                hw.onResult = { results ->
                    suggestions = results
                    suggestionSource = SuggestionSource.HANDWRITING
                    refreshTopBar()
                }
                hw.onAutoCommit = {
                    if (suggestionSource == SuggestionSource.HANDWRITING) {
                        suggestions.firstOrNull()?.let { output(it) }
                        suggestions = emptyList()
                        refreshTopBar()
                    }
                }
                content.addView(hw, 0, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                handwriting = hw
            }
            hw.side.setLayout(KeyLayouts.handwritingSide(returnTitle, nextModeTitle))
        } else {
            handwriting?.let { content.removeView(it) }
            handwriting = null
            if (suggestionSource == SuggestionSource.HANDWRITING) clearSuggestions()
        }
        if (panelView == null) {
            keyboard.visibility = if (showHandwriting) View.GONE else View.VISIBLE
        } else {
            handwriting?.visibility = View.GONE
        }
        candidatePanel?.bringToFront()
        content.layoutParams = content.layoutParams.apply { height = contentHeight() }
    }

    // ===== 回饋、提醒 =====

    private fun feedback() {
        if (settings.keySound) (getSystemService(Context.AUDIO_SERVICE) as AudioManager).playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f)
        if (settings.haptics) root?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
    }

    private var toastToken = 0

    private fun toast(text: String) {
        if (root == null) return
        val token = ++toastToken
        toastView.text = text
        toastView.bringToFront()
        toastView.animate().alpha(1f).setDuration(150).start()
        mainHandler.postDelayed({
            if (toastToken == token) toastView.animate().alpha(0f).setDuration(300).start()
        }, 1800)
    }

    /** 每 6 小時最多問一次 GitHub；有新版就提醒一次 */
    private fun checkForUpdate() {
        val now = System.currentTimeMillis()
        if (now - settings.lastUpdateCheck < 6 * 3600 * 1000L) {
            showUpdateNotice()
            return
        }
        settings.lastUpdateCheck = now
        UpdateFetcher.fetch { info ->
            if (info != null) {
                settings.latestBuild = info.build
                showUpdateNotice()
            }
        }
    }

    private fun showUpdateNotice() {
        val latest = settings.latestBuild
        if (latest <= BuildInfo.current(this) || latest <= settings.notifiedBuild) return
        settings.notifiedBuild = latest
        toast("有新版（Android 第 $latest 版）！打開星星鍵盤 App 更新")
    }
}
