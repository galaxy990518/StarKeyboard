package com.startown.starkeyboard.ime

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import java.io.File

/** 和 iPhone 版 Keyboard/Theme.swift 一樣的主題 */
data class KeyboardTheme(
    val id: String,
    val name: String,
    val top: Int,
    val bottom: Int,
    val key: Int,
    val keyText: Int,
    val special: Int,
    val specialText: Int,
    val accent: Int,
    val accentText: Int,
    val shadow: Int,
    val decoration: String,
    val dark: Boolean,
) {
    companion object {
        private fun c(hex: String) = Color.parseColor(hex)

        val all = listOf(
            KeyboardTheme("strawberry", "草莓牛奶", c("#FFE4EC"), c("#FFD0DE"), c("#FFFFFF"), c("#8A4B5C"), c("#FFC2D4"), c("#7A3045"), c("#FF7FA6"), c("#FFFFFF"), c("#E8A2B6"), "🍓", false),
            KeyboardTheme("mint", "薄荷蘇打", c("#DDF7EF"), c("#C5EFE3"), c("#FFFFFF"), c("#2F6B5E"), c("#A7E3D2"), c("#2F6B5E"), c("#3CC5A0"), c("#FFFFFF"), c("#9FD8C7"), "🌿", false),
            KeyboardTheme("milktea", "奶茶布丁", c("#F3E6D6"), c("#E9D5BD"), c("#FFF8EE"), c("#6B4A33"), c("#D9B994"), c("#5A3B24"), c("#B9835A"), c("#FFFFFF"), c("#CBAE8C"), "🍮", false),
            KeyboardTheme("lavender", "薰衣草", c("#ECE4FF"), c("#DCD0FF"), c("#FFFFFF"), c("#5B4B8A"), c("#C7B8F5"), c("#4A3B78"), c("#8D74E8"), c("#FFFFFF"), c("#B9A9EC"), "💜", false),
            KeyboardTheme("lemon", "檸檬小雞", c("#FFF6CC"), c("#FFEC9E"), c("#FFFFFF"), c("#7A5B12"), c("#FFDD6B"), c("#6B4E0A"), c("#FFA928"), c("#FFFFFF"), c("#E9CB6A"), "🐥", false),
            KeyboardTheme("night", "星空夜", c("#1E2245"), c("#2B2F63"), c("#3A3F7A"), c("#F3F0FF"), c("#2A2E5E"), c("#CFCBFF"), c("#FFD65C"), c("#2B2F63"), c("#14173A"), "⭐", true),
            KeyboardTheme("light", "經典淺色", c("#D3D6DC"), c("#CDD0D6"), c("#FFFFFF"), c("#000000"), c("#ABB0BA"), c("#000000"), c("#2F7BF6"), c("#FFFFFF"), c("#8E9196"), "", false),
            KeyboardTheme("dark", "經典深色", c("#2B2B2D"), c("#232325"), c("#6B6B6E"), c("#FFFFFF"), c("#474749"), c("#FFFFFF"), c("#2F7BF6"), c("#FFFFFF"), c("#111111"), "", true),
        )

        fun named(id: String) = all.firstOrNull { it.id == id } ?: all[0]
    }
}

fun Int.withAlpha(a: Float): Int = Color.argb((Color.alpha(this) * a.coerceIn(0f, 1f)).toInt(), Color.red(this), Color.green(this), Color.blue(this))

/** 按鍵造型（和 iPhone 版 KeyShape 一樣的算法，單位是 dp 換成的像素） */
enum class KeyShape(val title: String) {
    ROUNDED("圓角"), JELLY("果凍"), CAT_EAR("貓耳"), CLOUD("雲朵");

    private fun earSize(r: RectF, dp: Float) = minOf(r.width() * 0.24f, r.height() * 0.24f, 9 * dp)
    private fun bumpSize(r: RectF, dp: Float) = minOf(r.height() * 0.16f, 6 * dp)

    /** 按鍵本體（字畫在這裡面） */
    fun bodyRect(r: RectF, dp: Float): RectF = when (this) {
        CAT_EAR -> RectF(r.left, r.top + earSize(r, dp) * 0.6f, r.right, r.bottom)
        CLOUD -> RectF(r.left, r.top + bumpSize(r, dp), r.right, r.bottom)
        else -> RectF(r)
    }

    fun path(r: RectF, dp: Float): Path {
        val body = bodyRect(r, dp)
        val path = Path()
        val minSide = minOf(body.width(), body.height())
        when (this) {
            ROUNDED -> path.addRoundRect(body, minOf(7 * dp, body.height() * 0.22f), minOf(7 * dp, body.height() * 0.22f), Path.Direction.CW)
            JELLY -> path.addRoundRect(body, minSide * 0.42f, minSide * 0.42f, Path.Direction.CW)
            CAT_EAR -> {
                path.addRoundRect(body, minSide * 0.34f, minSide * 0.34f, Path.Direction.CW)
                val ear = earSize(r, dp)
                val left = Path().apply {
                    moveTo(body.left + ear * 0.2f, body.top + ear * 1.1f)
                    lineTo(body.left + ear * 0.45f, r.top)
                    lineTo(body.left + ear * 1.7f, body.top + ear * 0.35f)
                    close()
                }
                val right = Path().apply {
                    moveTo(body.right - ear * 1.7f, body.top + ear * 0.35f)
                    lineTo(body.right - ear * 0.45f, r.top)
                    lineTo(body.right - ear * 0.2f, body.top + ear * 1.1f)
                    close()
                }
                path.op(left, Path.Op.UNION)
                path.op(right, Path.Op.UNION)
            }
            CLOUD -> {
                path.addRoundRect(body, minSide * 0.38f, minSide * 0.38f, Path.Direction.CW)
                val b = bumpSize(r, dp)
                val count = maxOf(2, (body.width() / (20 * dp)).toInt())
                val step = body.width() / (count + 1)
                for (i in 1..count) {
                    val circle = Path().apply { addCircle(body.left + step * i, body.top + b * 0.5f, b * 1.5f, Path.Direction.CW) }
                    path.op(circle, Path.Op.UNION)
                }
            }
        }
        return path
    }
}

data class KeyboardStyle(
    val theme: KeyboardTheme,
    val shape: KeyShape,
    val keyAlpha: Float,
    val fontScale: Float,
    val gapScale: Float,
)

enum class InputMode(val title: String, val short: String, val settingName: String) {
    ZHUYIN("注音", "中", "注音"),
    ENGLISH("English", "英", "英文"),
    HANDWRITING("手寫", "寫", "手寫"),
    KOREAN("한국어", "한", "韓文（한국어）"),
    JAPANESE("日本語", "あ", "日文（羅馬拼音→假名）"),
    PINYIN("拼音", "拼", "拼音（馬來西亞、中國用，可出簡體）"),
}

/** 鍵盤設定（SharedPreferences） */
class KeyboardSettings(context: Context) {
    private val p = context.getSharedPreferences("keyboard", Context.MODE_PRIVATE)
    val dir: File = context.filesDir

    var themeId: String
        get() = p.getString("themeId", "strawberry")!!
        set(v) = p.edit().putString("themeId", v).apply()
    var shape: KeyShape
        get() = runCatching { KeyShape.valueOf(p.getString("shape", "JELLY")!!) }.getOrDefault(KeyShape.JELLY)
        set(v) = p.edit().putString("shape", v.name).apply()
    var keyAlpha: Float
        get() = p.getFloat("keyAlpha", 1f)
        set(v) = p.edit().putFloat("keyAlpha", v).apply()
    var backgroundFade: Float
        get() = p.getFloat("backgroundFade", 0.25f)
        set(v) = p.edit().putFloat("backgroundFade", v).apply()
    /** 照片裁切時盡量放在中間的點（0～1）；挑照片時有找到臉就是臉的位置 */
    var backgroundFocusX: Float
        get() = p.getFloat("backgroundFocusX", 0.5f)
        set(v) = p.edit().putFloat("backgroundFocusX", v).apply()
    var backgroundFocusY: Float
        get() = p.getFloat("backgroundFocusY", 0.5f)
        set(v) = p.edit().putFloat("backgroundFocusY", v).apply()
    var heightScale: Float
        get() = p.getFloat("heightScale", 1f)
        set(v) = p.edit().putFloat("heightScale", v).apply()
    var fontScale: Float
        get() = p.getFloat("fontScale", 1f)
        set(v) = p.edit().putFloat("fontScale", v).apply()
    var gapScale: Float
        get() = p.getFloat("gapScale", 1f)
        set(v) = p.edit().putFloat("gapScale", v).apply()
    var keySound: Boolean
        get() = p.getBoolean("keySound", true)
        set(v) = p.edit().putBoolean("keySound", v).apply()
    var haptics: Boolean
        get() = p.getBoolean("haptics", true)
        set(v) = p.edit().putBoolean("haptics", v).apply()
    var associations: Boolean
        get() = p.getBoolean("associations", true)
        set(v) = p.edit().putBoolean("associations", v).apply()
    var autocorrect: Boolean
        get() = p.getBoolean("autocorrect", true)
        set(v) = p.edit().putBoolean("autocorrect", v).apply()
    var fuzzyKeys: Boolean
        get() = p.getBoolean("fuzzyKeys", true)
        set(v) = p.edit().putBoolean("fuzzyKeys", v).apply()
    var fuzzyTones: Boolean
        get() = p.getBoolean("fuzzyTones", true)
        set(v) = p.edit().putBoolean("fuzzyTones", v).apply()
    var fuzzyPhonetic: Boolean
        get() = p.getBoolean("fuzzyPhonetic", false)
        set(v) = p.edit().putBoolean("fuzzyPhonetic", v).apply()
    var enabledModes: List<InputMode>
        get() {
            val raw = p.getString("modes", "ZHUYIN,ENGLISH,HANDWRITING")!!.split(',')
            return InputMode.values().filter { it.name in raw }.ifEmpty { listOf(InputMode.ZHUYIN) }
        }
        set(v) = p.edit().putString("modes", v.joinToString(",") { it.name }).apply()
    var lastMode: InputMode
        get() = runCatching { InputMode.valueOf(p.getString("lastMode", "ZHUYIN")!!) }.getOrDefault(InputMode.ZHUYIN)
        set(v) = p.edit().putString("lastMode", v.name).apply()
    var clipLimit: Int
        get() = p.getInt("clipLimit", 200)
        set(v) = p.edit().putInt("clipLimit", v).apply()
    /** 拼音模式輸出簡體字（馬來西亞、中國用簡體） */
    var pinyinSimplified: Boolean
        get() = p.getBoolean("pinyinSimplified", true)
        set(v) = p.edit().putBoolean("pinyinSimplified", v).apply()
    var translateTarget: String
        get() = p.getString("translateTarget", "en")!!
        set(v) = p.edit().putString("translateTarget", v).apply()
    var lastUpdateCheck: Long
        get() = p.getLong("lastUpdateCheck", 0)
        set(v) = p.edit().putLong("lastUpdateCheck", v).apply()
    var latestBuild: Int
        get() = p.getInt("latestBuild", 0)
        set(v) = p.edit().putInt("latestBuild", v).apply()
    var notifiedBuild: Int
        get() = p.getInt("notifiedBuild", 0)
        set(v) = p.edit().putInt("notifiedBuild", v).apply()

    val backgroundFile get() = File(dir, "background.jpg")
    var hasBackground: Boolean
        get() = p.getBoolean("hasBackground", false) && backgroundFile.exists()
        set(v) = p.edit().putBoolean("hasBackground", v).apply()

    val style get() = KeyboardStyle(KeyboardTheme.named(themeId), shape, keyAlpha, fontScale, gapScale)
}
