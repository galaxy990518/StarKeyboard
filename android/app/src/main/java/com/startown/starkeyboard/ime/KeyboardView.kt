package com.startown.starkeyboard.ime

import com.startown.starkeyboard.core.Pt
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/** 一整片按鍵：自己畫、自己處理觸控（多指連打、滑動修正、刪除鍵連續刪、空白鍵拖曳移動游標） */
@SuppressLint("ViewConstructor")
class KeyboardView(context: Context, style: KeyboardStyle) : View(context) {
    var onKey: ((KeySpec) -> Unit)? = null
    var onLongPress: ((KeySpec) -> Unit)? = null
    var onCursorMove: ((Int) -> Unit)? = null
    /** 按下去時上面跳出放大的字（看得到自己按到哪一顆） */
    var showsPreview = false
    private var previewKey: Key? = null
    private val previewPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val previewText = Paint(Paint.ANTI_ALIAS_FLAG).also { it.textAlign = Paint.Align.CENTER }

    private fun previewFor(k: Key?): Key? =
        if (showsPreview && k != null && k.spec.action.isCharacter && k.spec.label.length in 1..2) k else null
    /** 按住空白鍵上下滑：游標換到上一行（-1）或下一行（1） */
    var onCursorLine: ((Int) -> Unit)? = null
    /** 英文滑行輸入：手指滑過三個以上字母鍵才放開時，把畫的線交出去 */
    var glideEnabled = false
    var onGlide: ((List<Pt>, Map<Char, Pt>, Float) -> Unit)? = null

    var style: KeyboardStyle = style
        set(value) {
            field = value
            rebuildPaths()
            invalidate()
        }

    private val dp = resources.displayMetrics.density
    private var layout = KeyboardLayout(emptyList(), 10f)

    private class Key(val spec: KeySpec, val frame: RectF) {
        var path: Path = Path()
        var shadow: Path = Path()
        var detail: Path = Path()
        var shading: LinearGradient? = null
        var body: RectF = RectF()
        var detailAlpha = 0.4f
        var depth = 0f   // 立體：按鍵側面的厚度
        var releaseAt = 0L   // 放開的時間（果凍、跳跳、搖搖放開後還會動一下）
        var scale = 1f
        var pressed = false
        var animator: ValueAnimator? = null
    }

    private var keys: List<Key> = emptyList()

    private class Track(var key: Key, val action: KeyAction, val startX: Float, val startY: Float = 0f) {
        var cursorY = 0f
        var fired = false
        val points = ArrayList<Pt>()   // 滑行輸入：手指經過的點
        var visited = 1                // 經過幾個不同的鍵
        var gliding = false
        var cursorMode = false
        var cursorX = 0f
    }

    private val tracks = HashMap<Int, Track>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var repeatRunnable: Runnable? = null
    private var longPressRunnable: Runnable? = null

    /** 按鍵特效：從按鍵往上飄、慢慢變透明的小圖案 */
    private class Particle(
        val glyph: String, val x: Float, val y: Float, val dx: Float, val dy: Float,
        val rotation: Float, val size: Float, val color: Int, val start: Long,
    )
    private val particles = ArrayList<Particle>()
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        setStyle(Paint.Style.STROKE); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }

    private val Key.isLetter get() = spec.action is KeyAction.Text && spec.label.length == 1 && spec.label[0].lowercaseChar() in 'a'..'z'

    /** 每個字母鍵的中心（滑行輸入用） */
    private fun letterCenters(): Map<Char, Pt> =
        keys.filter { it.isLetter }.associate { it.spec.label[0].lowercaseChar() to Pt(it.frame.centerX(), it.frame.centerY()) }

    private fun drawTrail(canvas: Canvas) {
        for (t in tracks.values) {
            if (!t.gliding || t.points.size < 2) continue
            val pts = t.points.takeLast(40)
            val path = Path()
            path.moveTo(pts[0].x, pts[0].y)
            for (p in pts.drop(1)) path.lineTo(p.x, p.y)
            trailPaint.color = style.theme.accent.withAlpha(0.55f)
            trailPaint.strokeWidth = 5 * dp
            canvas.drawPath(path, trailPaint)
        }
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).also { it.style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    fun setLayout(newLayout: KeyboardLayout) {
        layout = newLayout
        rebuildKeys()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        rebuildKeys()
    }

    private fun rebuildKeys() {
        val w = width.toFloat()
        val h = height.toFloat()
        // 換版面時手指還按著：記住哪個位置被按著
        val pressedIndex = keys.indices.filter { keys[it].pressed }.toSet()
        val list = ArrayList<Key>()
        if (layout.rows.isNotEmpty() && w > 0 && h > 0) {
            val padX = 2 * dp
            val padY = 3 * dp
            val rowH = (h - padY * 2) / layout.rows.size
            val unit = (w - padX * 2) / layout.columns
            for ((r, row) in layout.rows.withIndex()) {
                val fixed = row.sumOf { maxOf(it.width, 0f).toDouble() }.toFloat()
                val flexCount = row.count { it.width == 0f }
                val flexW = if (flexCount > 0) maxOf(1f, (layout.columns - fixed) / flexCount) else 0f
                val total = fixed + flexW * flexCount
                var x = padX + maxOf(0f, layout.columns - total) * unit / 2
                val y = padY + rowH * r
                for (spec in row) {
                    val kw = (if (spec.width > 0) spec.width else flexW) * unit
                    list.add(Key(spec, RectF(x, y, x + kw, y + rowH)))
                    x += kw
                }
            }
        }
        keys = list
        // 按著的鍵換成新版面的同一個位置
        for (track in tracks.values) {
            val i = keys.indexOfFirst { it.frame.contains(track.key.frame.centerX(), track.key.frame.centerY()) }
            if (i >= 0) track.key = keys[i]
        }
        pressedIndex.forEach { if (it < keys.size) keys[it].pressed = true }
        rebuildPaths()
        invalidate()
    }

    private fun rebuildPaths() {
        for (k in keys) {
            // 線條造型（貓咪、熊熊…）有自己的形狀，空隙留小一點，圖案才夠大
            val art = style.shape.art != null
            val outer = RectF(k.frame).apply { inset((if (art) 1.2f else 3f) * dp * style.gapScale, (if (art) 1.8f else 4.5f) * dp * style.gapScale) }
            // 「造型大小」：造型可以畫得比按鍵大一點（寬的空白鍵不放大，才不會蓋到上一排）
            if (art && outer.width() < outer.height() * 1.65f) {
                val grow = style.shapeScale - 1f
                outer.inset(-outer.width() * grow / 2, -outer.height() * grow / 2)
            }
            if (outer.width() < 4 || outer.height() < 4) continue
            // 立體：按鍵往上留出側面的厚度
            k.depth = if (style.finish == KeyFinish.RAISED) minOf(4.5f * dp, outer.height() * 0.12f) else 0f
            outer.bottom -= k.depth
            k.path = style.shape.path(outer, dp)
            k.detail = style.shape.detailPath(outer, dp)
            val a = style.keyAlpha
            val dark = style.theme.dark
            val shadeColors = when (style.finish) {
                KeyFinish.FLAT -> intArrayOf(Color.WHITE.withAlpha(a * 0.30f), Color.TRANSPARENT, Color.BLACK.withAlpha(a * 0.04f))
                KeyFinish.PUFFY -> intArrayOf(Color.WHITE.withAlpha(a * 0.5f), Color.TRANSPARENT, Color.BLACK.withAlpha(a * (if (dark) 0.22f else 0.10f)))
                // 立體：按鍵調得很透明也看得到上亮下暗
                KeyFinish.RAISED -> maxOf(a, 0.6f).let { v -> intArrayOf(Color.WHITE.withAlpha(v * (if (dark) 0.3f else 0.55f)), Color.TRANSPARENT, Color.BLACK.withAlpha(v * (if (dark) 0.25f else 0.12f))) }
            }
            k.shading = if (style.shape.isSoftShape || style.finish != KeyFinish.FLAT) LinearGradient(0f, outer.top, 0f, outer.bottom,
                shadeColors, floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP) else null
            k.shadow = Path(k.path).apply { offset(0f, if (k.depth > 0) k.depth else (if (style.finish == KeyFinish.PUFFY) 3f else 1.5f) * dp) }
            k.body = style.shape.bodyRect(outer, dp)
            k.detailAlpha = style.shape.detailOpacity(outer)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val t = style.theme
        val now = android.os.SystemClock.uptimeMillis()
        val motion = KeyMotion.from(style.motion)
        var moving = false
        // 正在動的鍵最後畫，才不會被旁邊的鍵蓋住
        for (k in if (motion != KeyMotion.NONE) keys.sortedBy { if (it.scale != 1f || now - it.releaseAt < 600) 1 else 0 } else keys) {
            val fill: Int
            val text: Int
            when (k.spec.kind) {
                KeyKind.NORMAL -> { fill = if (k.pressed) t.special else t.key; text = t.keyText }
                KeyKind.SPECIAL -> { fill = if (k.pressed) t.key else t.special; text = t.specialText }
                KeyKind.ACCENT -> { fill = if (k.pressed) t.key else t.accent; text = if (k.pressed) t.keyText else t.accentText }
            }
            canvas.save()
            val finish = style.finish
            // 按下去的程度 0～1
            val press = ((1f - k.scale) * 10f).coerceIn(0f, 1f)
            val m = if (k.spec.action.isCharacter) motion else KeyMotion.NONE
            if (m != KeyMotion.NONE && (k.pressed || now - k.releaseAt < 600)) {
                if (applyMotion(canvas, k, m, press, now)) moving = true
            } else if (k.scale != 1f) {
                val d = 1f - k.scale
                // 胖胖的鍵按下去會被壓扁；立體的鍵往下沉
                if (finish == KeyFinish.PUFFY) canvas.scale(1f + d * 0.6f, 1f - d * 1.4f, k.frame.centerX(), k.frame.centerY())
                else if (finish == KeyFinish.RAISED) canvas.translate(0f, k.depth * press * 0.85f)
                else canvas.scale(k.scale, k.scale, k.frame.centerX(), k.frame.centerY())
            }
            val a = style.keyAlpha
            fillPaint.color = when (finish) {
                KeyFinish.FLAT -> t.shadow.withAlpha(if (style.shape.isSoftShape) a * 0.32f else minOf(1f, a + 0.1f))
                KeyFinish.PUFFY -> t.shadow.withAlpha(minOf(1f, a * 0.75f))
                // 側面用按鍵本身的顏色調暗，透明的鍵也一定看得到
                KeyFinish.RAISED -> darker(fill, if (t.dark) 0.55f else 0.78f).withAlpha(maxOf(a, 0.85f) * (1f - press))
            }
            // 影子只畫在按鍵外面，按鍵調透明時才不會看到兩層疊在一起
            canvas.save()
            canvas.clipOutPath(k.path)
            canvas.drawPath(k.shadow, fillPaint)
            canvas.restore()
            fillPaint.color = fill.withAlpha(a)
            canvas.drawPath(k.path, fillPaint)
            if (finish == KeyFinish.PUFFY) {
                // 左上角一顆圓圓的反光，看起來鼓鼓的
                val b = k.body
                fillPaint.color = Color.WHITE.withAlpha(if (t.dark) 0.3f else 0.75f)
                canvas.drawOval(RectF(b.left + b.width() * 0.14f, b.top + b.height() * 0.1f, b.left + b.width() * 0.46f, b.top + b.height() * 0.3f), fillPaint)
            } else if (style.shape == KeyShape.JELLY) {
                val b = k.body
                val gh = maxOf(2.5f * dp, b.height() * 0.16f)
                fillPaint.color = android.graphics.Color.WHITE.withAlpha(if (t.dark) 0.18f else 0.55f)
                val gloss = RectF(b.left + b.width() * 0.2f, b.top + 2.5f * dp, b.right - b.width() * 0.2f, b.top + 2.5f * dp + gh)
                canvas.drawRoundRect(gloss, gh / 2, gh / 2, fillPaint)
            }
            k.shading?.let { shader ->
                fillPaint.color = Color.WHITE
                fillPaint.shader = shader
                canvas.drawPath(k.path, fillPaint)
                fillPaint.shader = null
            }
            fillPaint.color = style.shape.detailColor(t).withAlpha(style.keyAlpha * k.detailAlpha)
            canvas.drawPath(k.detail, fillPaint)
            drawLabel(canvas, k, text)
            canvas.restore()
        }
        if (moving) postInvalidateOnAnimation()
        drawTrail(canvas)
        drawParticles(canvas)
    }

    /** 放大泡泡：在按鍵上方畫一個大大的字（最上排就往下一點，不要被切掉） */
    private fun drawPreview(canvas: Canvas, k: Key) {
        val t = style.theme
        val f = k.frame
        val w = maxOf(f.width() * 1.45f, 42 * dp)
        val h = f.height() * 1.2f
        val left = (f.centerX() - w / 2).coerceIn(2 * dp, width - w - 2 * dp)
        val top = maxOf(0f, f.top - h + 4 * dp)
        val r = RectF(left, top, left + w, top + h)
        val radius = minOf(w, h) * 0.42f
        previewPaint.style = Paint.Style.FILL
        previewPaint.color = t.shadow.withAlpha(0.5f)
        canvas.drawRoundRect(RectF(r).apply { offset(0f, 2 * dp) }, radius, radius, previewPaint)
        previewPaint.color = t.key
        canvas.drawRoundRect(r, radius, radius, previewPaint)
        previewPaint.style = Paint.Style.STROKE
        previewPaint.strokeWidth = 1.5f * dp
        previewPaint.color = t.accent.withAlpha(0.6f)
        canvas.drawRoundRect(r, radius, radius, previewPaint)
        previewText.color = t.keyText
        previewText.textSize = h * 0.6f
        previewText.typeface = KeyFonts.get(context, style.fontId) ?: Typeface.DEFAULT_BOLD
        val fm = previewText.fontMetrics
        canvas.drawText(k.spec.label, r.centerX(), r.centerY() - (fm.ascent + fm.descent) / 2, previewText)
    }

    /** 可愛的按鍵動作；回傳 true 表示還在動（要繼續重畫） */
    private fun applyMotion(canvas: Canvas, k: Key, m: KeyMotion, press: Float, now: Long): Boolean {
        val f = k.frame
        val h = f.height()
        val cx = f.centerX()
        if (k.pressed) {
            when (m) {
                KeyMotion.POP -> { canvas.translate(0f, -h * 0.22f * press); canvas.scale(1f + 0.25f * press, 1f + 0.25f * press, cx, f.centerY()) }
                KeyMotion.JELLY -> canvas.scale(1f + 0.18f * press, 1f - 0.2f * press, cx, f.bottom)   // 貼著地板壓扁
                KeyMotion.HOP -> canvas.scale(1f + 0.06f * press, 1f - 0.1f * press, cx, f.bottom)
                KeyMotion.WIGGLE -> { canvas.rotate(9f * press, cx, f.centerY()); canvas.scale(1f + 0.08f * press, 1f + 0.08f * press, cx, f.centerY()) }
                KeyMotion.NONE -> {}
            }
            return false
        }
        val ms = (now - k.releaseAt).toFloat()
        when (m) {
            KeyMotion.JELLY -> {
                // 像果凍一樣 duang duang 抖回來：越抖越小
                val t = ms / 550f
                if (t >= 1f) return false
                val a = Math.exp(-4.5 * t).toFloat() * Math.cos(4.5 * Math.PI * t).toFloat()
                canvas.scale(1f + 0.18f * a, 1f - 0.2f * a, cx, f.bottom)
            }
            KeyMotion.HOP -> {
                // 蹦一下：往上跳（拉長）→ 落地壓扁 → 回來
                val t = ms / 420f
                if (t >= 1f) return false
                if (t < 0.65f) {
                    val u = t / 0.65f
                    canvas.translate(0f, -h * 0.45f * Math.sin(Math.PI * u).toFloat())
                    canvas.scale(0.92f, 1.12f, cx, f.centerY())
                } else {
                    val s = Math.sin(Math.PI * (t - 0.65f) / 0.35f).toFloat()
                    canvas.scale(1f + 0.15f * s, 1f - 0.15f * s, cx, f.bottom)
                }
            }
            KeyMotion.WIGGLE -> {
                // 左右扭一扭
                val t = ms / 400f
                if (t >= 1f) return false
                val angle = 12f * Math.exp(-3.5 * t).toFloat() * Math.sin(5 * Math.PI * t).toFloat()
                canvas.rotate(-angle, cx, f.centerY())
            }
            KeyMotion.POP -> {
                // 放大的那種很快縮回去
                val t = ms / 90f
                if (t >= 1f) return false
                val s = 1f + 0.25f * (1f - t)
                canvas.translate(0f, -h * 0.22f * (1f - t))
                canvas.scale(s, s, cx, f.centerY())
            }
            KeyMotion.NONE -> return false
        }
        return true
    }

    private fun darker(c: Int, f: Float) = Color.rgb((Color.red(c) * f).toInt(), (Color.green(c) * f).toInt(), (Color.blue(c) * f).toInt())

    // 空白鍵的圖（GIF 會動）：換圖時才重新讀
    private var spaceArt: android.graphics.drawable.Drawable? = null
    private var spaceArtKey = ""
    // Drawable 只記得 callback 的弱參照，要自己留著
    private val spaceArtCallback = object : android.graphics.drawable.Drawable.Callback {
        override fun invalidateDrawable(who: android.graphics.drawable.Drawable) { invalidate() }
        override fun scheduleDrawable(who: android.graphics.drawable.Drawable, what: Runnable, `when`: Long) { postDelayed(what, `when` - android.os.SystemClock.uptimeMillis()) }
        override fun unscheduleDrawable(who: android.graphics.drawable.Drawable, what: Runnable) { removeCallbacks(what) }
    }

    private fun drawSpaceArt(canvas: Canvas, k: Key): Boolean {
        if (!k.spec.label.startsWith(SpaceArt.MARK)) return false
        if (spaceArtKey != k.spec.label) {
            spaceArtKey = k.spec.label
            (spaceArt as? android.graphics.drawable.Animatable)?.stop()
            spaceArt = SpaceArt.load(context)?.also { d ->
                d.callback = spaceArtCallback
                (d as? android.graphics.drawable.Animatable)?.start()
            }
        }
        val d = spaceArt ?: return false
        // 小小的放在正中間，維持原本的比例
        val h = k.body.height() * 0.8f
        val iw = d.intrinsicWidth.coerceAtLeast(1).toFloat(); val ih = d.intrinsicHeight.coerceAtLeast(1).toFloat()
        val w = minOf(h * iw / ih, k.body.width() * 0.6f)
        val hh = w * ih / iw
        d.setBounds((k.body.centerX() - w / 2).toInt(), (k.body.centerY() - hh / 2).toInt(), (k.body.centerX() + w / 2).toInt(), (k.body.centerY() + hh / 2).toInt())
        d.draw(canvas)
        return true
    }

    private fun drawLabel(canvas: Canvas, k: Key, color: Int) {
        if (drawSpaceArt(canvas, k)) return
        textPaint.color = color
        textPaint.typeface = KeyFonts.get(context, style.fontId)
            ?: if (k.spec.kind == KeyKind.NORMAL) Typeface.DEFAULT else Typeface.DEFAULT_BOLD
        var size = k.spec.fontSize * dp * style.fontScale
        val art = style.shape.art != null
        if (style.shape.isSoftShape) {
            // 線條造型的字可以比身體框大一點，「字的大小」才調得動
            size = minOf(size, k.body.height() * (if (art) 1.4f else 0.85f))
        }
        textPaint.textSize = size
        val maxW = if (art) k.body.width() * 1.3f else k.body.width() - 4 * dp
        val w = textPaint.measureText(k.spec.label)
        if (w > maxW && w > 0) {
            size *= maxW / w
            textPaint.textSize = size
        }
        val fm = textPaint.fontMetrics
        val baseline = k.body.centerY() - (fm.ascent + fm.descent) / 2
        canvas.drawText(k.spec.label, k.body.centerX(), baseline, textPaint)
    }

    // ===== 觸控 =====

    private fun keyAt(x: Float, y: Float): Key? {
        keys.firstOrNull { it.frame.contains(x, y) }?.let { return it }
        return keys.minByOrNull { k ->
            val dx = maxOf(k.frame.left - x, 0f, x - k.frame.right)
            val dy = maxOf(k.frame.top - y, 0f, y - k.frame.bottom)
            dx * dx + dy * dy
        }
    }

    private fun spawnEffect(k: Key) {
        val glyphs = KeyEffect.from(style.keyEffect).glyphs
        if (glyphs.isEmpty() || particles.size > 90) return
        val now = android.os.SystemClock.uptimeMillis()
        val t = style.theme
        repeat(3) { i ->
            particles.add(Particle(
                glyphs.random(), k.frame.centerX(), k.frame.centerY(),
                (Math.random().toFloat() - 0.5f) * 60 * dp, -(45 + Math.random().toFloat() * 30) * dp,
                (Math.random().toFloat() - 0.5f) * 70f, (15 + Math.random().toFloat() * 9) * dp,
                if (i == 0) t.accent else t.keyText, now,
            ))
        }
        invalidate()
    }

    private fun drawParticles(canvas: Canvas) {
        if (particles.isEmpty()) return
        val now = android.os.SystemClock.uptimeMillis()
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            // 飄久一點（0.8 秒），前半段清楚看得到，後半段才慢慢變透明
            val t = (now - p.start) / 800f
            if (t >= 1f) { it.remove(); continue }
            val ease = 1 - (1 - t) * (1 - t)
            val x = p.x + p.dx * ease
            val y = p.y + p.dy * ease
            particlePaint.color = p.color.withAlpha(if (t < 0.5f) 1f else 1f - (t - 0.5f) / 0.5f)
            particlePaint.textSize = p.size * (1 + 0.3f * ease)
            canvas.save()
            canvas.rotate(p.rotation * ease, x, y)
            canvas.drawText(p.glyph, x, y, particlePaint)
            canvas.restore()
        }
        if (particles.isNotEmpty()) postInvalidateOnAnimation()
    }

    private fun setPressed(k: Key, pressed: Boolean) {
        if (k.pressed == pressed) return
        k.pressed = pressed
        if (pressed) spawnEffect(k) else k.releaseAt = android.os.SystemClock.uptimeMillis()
        k.animator?.cancel()
        val anim = if (pressed) {
            ValueAnimator.ofFloat(k.scale, 0.9f).setDuration(50)
        } else {
            ValueAnimator.ofFloat(k.scale, 1f).setDuration(80)
        }
        anim.addUpdateListener { k.scale = it.animatedValue as Float; invalidate() }
        anim.start()
        k.animator = anim
        invalidate()
    }

    private fun fire(k: Key) {
        onKey?.invoke(k.spec)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                val key = keyAt(e.getX(i), e.getY(i)) ?: return true
                // 先亮起這顆鍵，再送出上一拍還沒放開的字。選字不能擋住按鍵變色。
                setPressed(key, true)
                val track = Track(key, key.spec.action, e.getX(i), e.getY(i))
                previewKey = previewFor(key)
                if (glideEnabled && key.isLetter) track.points.add(Pt(e.getX(i), e.getY(i)))
                tracks[e.getPointerId(i)] = track
                for (t in tracks.values) {
                    if (t !== track && !t.fired && !t.cursorMode && !t.gliding && (t.action.isCharacter || t.action == KeyAction.Space)) {
                        t.fired = true
                        setPressed(t.key, false)
                        fire(t.key)
                    }
                }
                when (key.spec.action) {
                    KeyAction.Backspace -> {
                        track.fired = true
                        fire(key)
                        startRepeat(key)
                    }
                    KeyAction.Shift -> {
                        track.fired = true
                        fire(key)
                    }
                    KeyAction.NextMode -> {
                        val r = Runnable {
                            if (!track.fired) {
                                track.fired = true
                                setPressed(track.key, false)
                                onLongPress?.invoke(key.spec)
                            }
                        }
                        longPressRunnable = r
                        mainHandler.postDelayed(r, 500)
                    }
                    else -> {}
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val t = tracks[e.getPointerId(i)] ?: continue
                    if (t.fired) continue
                    val x = e.getX(i)
                    if (t.action == KeyAction.Space) {
                        val y = e.getY(i)
                        if (!t.cursorMode && (abs(x - t.startX) > 16 * dp || abs(y - t.startY) > 18 * dp)) {
                            t.cursorMode = true
                            t.cursorX = x
                            t.cursorY = y
                        }
                        if (t.cursorMode) {
                            val steps = ((x - t.cursorX) / (9 * dp)).toInt()
                            if (steps != 0) {
                                onCursorMove?.invoke(steps)
                                t.cursorX += steps * 9 * dp
                            }
                            // 上下滑一段（約 22dp）換一行
                            val lines = ((y - t.cursorY) / (22 * dp)).toInt()
                            if (lines != 0) {
                                val d = if (lines > 0) 1 else -1
                                onCursorLine?.invoke(d)
                                t.cursorY += d * 22 * dp
                            }
                        }
                    } else if (t.action.isCharacter) {
                        if (t.points.isNotEmpty()) {
                            t.points.add(Pt(x, e.getY(i)))
                            if (t.gliding) invalidate()
                        }
                        val k = keyAt(x, e.getY(i))
                        if (k != null && k !== t.key && k.spec.action.isCharacter) {
                            setPressed(t.key, false)
                            setPressed(k, true)
                            t.key = k
                            previewKey = previewFor(k)
                            invalidate()
                            if (t.points.isNotEmpty() && ++t.visited >= 3) t.gliding = true
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val t = tracks.remove(e.getPointerId(e.actionIndex)) ?: return true
                setPressed(t.key, false)
                if (tracks.isEmpty() || previewKey === t.key) { previewKey = null; invalidate() }
                if (t.action == KeyAction.Backspace) stopRepeat()
                if (t.action == KeyAction.NextMode) longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                if (t.gliding) {
                    t.points.add(Pt(e.getX(e.actionIndex), e.getY(e.actionIndex)))
                    val kw = keys.firstOrNull { it.isLetter }?.frame?.width() ?: (30 * dp)
                    onGlide?.invoke(t.points, letterCenters(), kw)
                    invalidate()
                } else if (!t.fired && !t.cursorMode && t.action != KeyAction.Backspace && t.action != KeyAction.Shift) fire(t.key)
            }
            MotionEvent.ACTION_CANCEL -> {
                val pending = tracks.values.filter { !it.fired && !it.cursorMode && !it.gliding && it.action.isCharacter }
                for (t in tracks.values) setPressed(t.key, false)
                tracks.clear()
                previewKey = null
                invalidate()
                stopRepeat()
                longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                for (t in pending) fire(t.key)
            }
        }
        return true
    }

    private fun startRepeat(k: Key) {
        stopRepeat()
        val r = object : Runnable {
            override fun run() {
                fire(k)
                mainHandler.postDelayed(this, 80)
            }
        }
        repeatRunnable = r
        mainHandler.postDelayed(r, 450)
    }

    private fun stopRepeat() {
        repeatRunnable?.let { mainHandler.removeCallbacks(it) }
        repeatRunnable = null
    }
}
