package com.startown.starkeyboard.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

fun View.dp(v: Float) = v * resources.displayMetrics.density
fun View.dpi(v: Float) = (v * resources.displayMetrics.density).toInt()

fun roundRect(color: Int, radius: Float) = GradientDrawable().apply {
    setColor(color)
    cornerRadius = radius
}

/** 一個可以點的文字（工具列、候選字、按鈕都用這個） */
@SuppressLint("ViewConstructor")
class Tap(context: Context, text: String, sizeSp: Float, color: Int, bold: Boolean = false, onTap: () -> Unit) : TextView(context) {
    init {
        this.text = text
        textSize = sizeSp
        setTextColor(color)
        gravity = Gravity.CENTER
        maxLines = 1
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setOnClickListener { onTap() }
    }
}

enum class Trailing { EXPAND, CLOSE }

enum class ToolbarItem(val label: String) {
    CLIPBOARD("剪貼"), EMOJI("☺︎"), KAOMOJI("(•ᴗ•)"), SYMBOLS("※"), TRANSLATE("譯"),
    HANDWRITING("✍︎"), SETTINGS("⚙︎"), DISMISS("▾"),
}

/** 鍵盤最上面那一條：工具列／候選字／編輯框（新增表符、翻譯） */
@SuppressLint("ViewConstructor")
class TopBar(context: Context, style: KeyboardStyle) : FrameLayout(context) {
    var onToolbar: ((ToolbarItem) -> Unit)? = null
    var onCandidate: ((Int) -> Unit)? = null
    var onTrailing: ((Trailing) -> Unit)? = null
    var onEditor: ((Boolean) -> Unit)? = null
    var onEditorExtra: (() -> Unit)? = null

    var style = style
        set(value) {
            field = value
            redraw()
        }

    private sealed class Content {
        object Toolbar : Content()
        data class Candidates(val composition: String, val items: List<String>, val trailing: Trailing) : Content()
        data class Editor(val prompt: String, val text: String, val extra: String, val confirm: String) : Content()
    }

    private var content: Content = Content.Toolbar

    fun showToolbar() { content = Content.Toolbar; redraw() }
    fun showCandidates(composition: String, items: List<String>, trailing: Trailing) {
        content = Content.Candidates(composition, items, trailing); redraw()
    }
    fun showEditor(prompt: String, text: String, extra: String, confirm: String) {
        content = Content.Editor(prompt, text, extra, confirm); redraw()
    }

    private val tint get() = if (style.theme.dark) style.theme.keyText else style.theme.specialText

    private fun redraw() {
        removeAllViews()
        val t = style.theme
        when (val c = content) {
            is Content.Toolbar -> {
                val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                for (item in ToolbarItem.values()) {
                    val size = if (item == ToolbarItem.KAOMOJI || item == ToolbarItem.CLIPBOARD) 14f else 19f
                    row.addView(Tap(context, item.label, size, tint, true) { onToolbar?.invoke(item) },
                        LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
                }
                addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                    leftMargin = dpi(4f); rightMargin = dpi(4f)
                })
            }
            is Content.Candidates -> {
                val top = if (c.composition.isNotEmpty()) dpi(15f) else 0
                if (c.composition.isNotEmpty()) {
                    addView(TextView(context).apply {
                        text = c.composition
                        textSize = 12f
                        setTextColor(t.keyText.withAlpha(0.75f))
                        maxLines = 1
                        ellipsize = TextUtils.TruncateAt.START
                    }, LayoutParams(LayoutParams.MATCH_PARENT, dpi(15f)).apply { leftMargin = dpi(12f); rightMargin = dpi(48f) })
                }
                val scroll = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false }
                val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                val first = if (t.accent == t.key) t.keyText else t.accent
                for ((i, s) in c.items.take(60).withIndex()) {
                    row.addView(Tap(context, s, 19f, if (i == 0) first else t.keyText, i == 0) { onCandidate?.invoke(i) }.apply {
                        setPadding(dpi(11f), 0, dpi(11f), 0)
                    }, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
                }
                scroll.addView(row, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
                addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                    topMargin = top; rightMargin = dpi(44f); leftMargin = dpi(4f)
                })
                if (c.items.isNotEmpty() || c.trailing == Trailing.CLOSE) {
                    addView(Tap(context, if (c.trailing == Trailing.EXPAND) "⌄" else "✕", 18f, tint, true) {
                        onTrailing?.invoke(c.trailing)
                    }, LayoutParams(dpi(44f), LayoutParams.MATCH_PARENT, Gravity.END))
                }
            }
            is Content.Editor -> {
                val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                row.addView(TextView(context).apply {
                    text = c.prompt + c.text + "▏"
                    textSize = 16f
                    setTextColor(t.keyText)
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.START
                    gravity = Gravity.CENTER_VERTICAL
                }, LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply { leftMargin = dpi(12f) })
                row.addView(Tap(context, c.extra, 14f, tint, true) { onEditorExtra?.invoke() }.apply { setPadding(dpi(8f), 0, dpi(8f), 0) },
                    LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
                row.addView(Tap(context, "取消", 15f, tint) { onEditor?.invoke(false) }, LinearLayout.LayoutParams(dpi(56f), LayoutParams.MATCH_PARENT))
                row.addView(Tap(context, c.confirm, 15f, if (t.dark) t.accent else t.specialText, true) { onEditor?.invoke(true) },
                    LinearLayout.LayoutParams(dpi(60f), LayoutParams.MATCH_PARENT))
                addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            }
        }
        addView(View(context).apply { setBackgroundColor(t.shadow.withAlpha(0.4f)) },
            LayoutParams(LayoutParams.MATCH_PARENT, maxOf(1, dpi(0.5f)), Gravity.BOTTOM))
    }

    init {
        redraw()
    }
}

/** 面板最下面那條：返回鍵盤、分頁、刪除（按住會一直刪） */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class PanelBottomBar(context: Context, private val style: KeyboardStyle, tabs: List<String>, selected: Int,
                     backTitle: String = "ABC", showBackspace: Boolean = true,
                     onBack: () -> Unit, onTab: (Int) -> Unit, onBackspace: () -> Unit) : LinearLayout(context) {
    private val tabViews = ArrayList<TextView>()
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        orientation = HORIZONTAL
        val t = style.theme
        setPadding(dpi(4f), dpi(3f), dpi(4f), dpi(3f))
        addView(Tap(context, backTitle, 15f, t.specialText, true, onBack).apply {
            background = roundRect(t.special.withAlpha(style.keyAlpha), dp(8f))
        }, LayoutParams(dpi(56f), LayoutParams.MATCH_PARENT))
        val scroll = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(context)
        for ((i, s) in tabs.withIndex()) {
            val v = Tap(context, s, if (s.codePointCount(0, s.length) <= 2) 18f else 14f, t.keyText, true) {
                select(i)
                onTab(i)
            }
            v.setPadding(dpi(10f), 0, dpi(10f), 0)
            v.minWidth = dpi(40f)
            tabViews.add(v)
            row.addView(v, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT).apply { rightMargin = dpi(2f) })
        }
        scroll.addView(row)
        addView(scroll, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply { leftMargin = dpi(4f); rightMargin = dpi(4f) })
        if (showBackspace) {
            val del = Tap(context, "⌫", 18f, t.specialText, true) {}.apply {
                background = roundRect(t.special.withAlpha(style.keyAlpha), dp(8f))
            }
            val repeat = object : Runnable {
                override fun run() {
                    onBackspace()
                    mainHandler.postDelayed(this, 80)
                }
            }
            del.setOnTouchListener { _, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { onBackspace(); mainHandler.postDelayed(repeat, 450) }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> mainHandler.removeCallbacks(repeat)
                }
                true
            }
            addView(del, LayoutParams(dpi(52f), LayoutParams.MATCH_PARENT))
        }
        select(selected)
    }

    fun select(i: Int) {
        val t = style.theme
        for ((j, v) in tabViews.withIndex()) {
            val on = j == i
            v.background = if (on) roundRect(t.key.withAlpha(style.keyAlpha), dp(8f)) else null
            v.setTextColor(if (on) t.keyText else t.keyText.withAlpha(0.6f))
        }
    }
}

/** 會自動換行的排列（顏文字、候選字用） */
class FlowLayout(context: Context) : ViewGroup(context) {
    private val gap = dpi(6f)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxW = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        var x = 0
        var y = 0
        var lineH = 0
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            c.measure(MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            if (x > 0 && x + c.measuredWidth > maxW) {
                x = 0
                y += lineH + gap
                lineH = 0
            }
            x += c.measuredWidth + gap
            lineH = maxOf(lineH, c.measuredHeight)
        }
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), y + lineH + paddingTop + paddingBottom)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val maxW = r - l - paddingLeft - paddingRight
        var x = 0
        var y = 0
        var lineH = 0
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (x > 0 && x + c.measuredWidth > maxW) {
                x = 0
                y += lineH + gap
                lineH = 0
            }
            c.layout(paddingLeft + x, paddingTop + y, paddingLeft + x + c.measuredWidth, paddingTop + y + c.measuredHeight)
            x += c.measuredWidth + gap
            lineH = maxOf(lineH, c.measuredHeight)
        }
    }
}

/** 可以捲動的方塊格子（表情符號、顏文字、候選字） */
class ChipGrid(context: Context, private val style: KeyboardStyle) : ScrollView(context) {
    sealed class Item {
        data class Text(val s: String) : Item()
        data class Image(val bitmap: Bitmap) : Item()
        object Add : Item()
    }

    var onSelect: ((Int) -> Unit)? = null
    var onLongPress: ((Int) -> Unit)? = null
    var canDelete: (Int) -> Boolean = { false }
    var editing = false
        set(value) {
            field = value
            render()
        }

    private val flow = FlowLayout(context).apply { setPadding(dpi(8f), dpi(6f), dpi(8f), dpi(6f)) }
    private var items: List<Item> = emptyList()
    private var square = true

    init {
        addView(flow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun setItems(newItems: List<Item>, squareCells: Boolean) {
        items = newItems
        square = squareCells
        render()
        scrollTo(0, 0)
    }

    private fun render() {
        flow.removeAllViews()
        val t = style.theme
        for ((i, item) in items.withIndex()) {
            val deletable = editing && canDelete(i)
            val view: View = when (item) {
                is Item.Text -> TextView(context).apply {
                    text = if (deletable) "✕ " + item.s else item.s
                    gravity = Gravity.CENTER
                    maxLines = 1
                    setTextColor(if (deletable) android.graphics.Color.parseColor("#FF5A6E") else t.keyText)
                    if (square && !deletable) {
                        textSize = 26f
                        minWidth = dpi(42f); minHeight = dpi(42f)
                    } else {
                        textSize = 17f
                        minWidth = dpi(44f); minHeight = dpi(40f)
                        setPadding(dpi(12f), 0, dpi(12f), 0)
                        background = roundRect(t.key.withAlpha(style.keyAlpha * 0.9f), dp(10f))
                    }
                }
                is Item.Image -> FrameLayout(context).apply {
                    background = roundRect(t.key.withAlpha(style.keyAlpha * 0.6f), dp(10f))
                    addView(ImageView(context).apply {
                        setImageBitmap(item.bitmap)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        if (deletable) alpha = 0.4f
                    }, FrameLayout.LayoutParams(dpi(62f), dpi(62f), Gravity.CENTER))
                    if (deletable) addView(TextView(context).apply {
                        text = "✕"; textSize = 20f; setTextColor(android.graphics.Color.parseColor("#FF5A6E")); gravity = Gravity.CENTER
                    }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                    minimumWidth = dpi(70f); minimumHeight = dpi(70f)
                }
                is Item.Add -> TextView(context).apply {
                    text = "＋"
                    textSize = 22f
                    gravity = Gravity.CENTER
                    setTextColor(t.specialText)
                    minWidth = dpi(64f); minHeight = dpi(40f)
                    background = roundRect(t.special.withAlpha(style.keyAlpha), dp(10f))
                }
            }
            view.setOnClickListener { onSelect?.invoke(i) }
            view.setOnLongClickListener { onLongPress?.invoke(i); true }
            flow.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }
}
