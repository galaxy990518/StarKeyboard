package com.startown.starkeyboard.core

import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * 花式文字：把字換成 Unicode 裡長得不一樣的字母，或加上線條、外框。
 * 傳出去對方也看得到（不是字體，是不同的字）。和 iPhone 版 FancyText.swift 一樣。
 */
object FancyText {
    private const val SMALL_CAPS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ"
    private const val UPSIDE_DOWN = "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz"

    /** 一個樣式：名稱＋轉換方法 */
    class Style(val name: String, val convert: (String) -> String)

    val styles = listOf(
        Style("草寫") { letters(it, 0x1D4D0, 0x1D4EA, null) },
        Style("粗體") { letters(it, 0x1D5D4, 0x1D5EE, 0x1D7EC) },
        Style("斜體") { letters(it, 0x1D608, 0x1D622, null) },
        Style("哥德") { letters(it, 0x1D56C, 0x1D586, null) },
        Style("打字機") { letters(it, 0x1D670, 0x1D68A, 0x1D7F6) },
        Style("圈圈") { circled(it) },
        Style("黑圈") { map(it) { c -> if (c.isAsciiLetter()) 0x1F150 + (c.uppercaseChar() - 'A') else null } },
        Style("方塊") { map(it) { c -> if (c.isAsciiLetter()) 0x1F170 + (c.uppercaseChar() - 'A') else null } },
        Style("小型大寫") { map(it) { c -> if (c in 'a'..'z' || c in 'A'..'Z') SMALL_CAPS[c.lowercaseChar() - 'a'].code else null } },
        Style("全形") { fullWidth(it) },
        Style("倒過來") { upsideDown(it) },
        Style("刪除線") { combine(it, '\u0336') },
        Style("底線") { combine(it, '\u0332') },
        Style("波浪") { combine(it, '\u0330') },
        Style("愛心") { "♡ $it ♡" },
        Style("閃亮") { "✧ $it ✧" },
        Style("星星") { "⋆｡˚ $it ˚｡⋆" },
        Style("花花") { "✿ $it ✿" },
        Style("括號") { "꒰ $it ꒱" },
        Style("標題") { "【$it】" },
        Style("一字一星") { spaced(it, " ✦ ") },
    )

    /** 所有和原文不一樣的樣式（文字沒有英文字母時，英文樣式就不會出現） */
    fun variants(text: String): List<String> =
        styles.map { it.convert(text) }.filter { it != text }.distinct()

    private fun Char.isAsciiLetter() = this in 'a'..'z' || this in 'A'..'Z'

    private fun map(text: String, f: (Char) -> Int?): String = buildString {
        for (c in text) {
            val code = f(c)
            if (code == null) append(c) else appendCodePoint(code)
        }
    }

    private fun letters(text: String, upper: Int, lower: Int, digit: Int?) = map(text) { c ->
        when (c) {
            in 'A'..'Z' -> upper + (c - 'A')
            in 'a'..'z' -> lower + (c - 'a')
            in '0'..'9' -> digit?.let { it + (c - '0') }
            else -> null
        }
    }

    private fun circled(text: String) = map(text) { c ->
        when (c) {
            in 'A'..'Z' -> 0x24B6 + (c - 'A')
            in 'a'..'z' -> 0x24D0 + (c - 'a')
            '0' -> 0x24EA
            in '1'..'9' -> 0x2460 + (c - '1')
            else -> null
        }
    }

    private fun fullWidth(text: String) = map(text) { c ->
        when (c) {
            ' ' -> 0x3000
            in '!'..'~' -> 0xFF01 + (c - '!')
            else -> null
        }
    }

    private fun upsideDown(text: String): String {
        if (text.none { it.isAsciiLetter() }) return text
        return map(text.reversed()) { c -> if (c.isAsciiLetter()) UPSIDE_DOWN[c.lowercaseChar() - 'a'].code else null }
    }

    /** 每個字後面加一個組合符號（刪除線、底線…），中文也可以 */
    private fun combine(text: String, mark: Char) = buildString {
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            appendCodePoint(cp)
            if (!Character.isWhitespace(cp)) append(mark)
            i += Character.charCount(cp)
        }
    }

    private fun spaced(text: String, sep: String): String {
        val chars = ArrayList<String>()
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            if (!Character.isWhitespace(cp)) chars.add(String(Character.toChars(cp)))
            i += Character.charCount(cp)
        }
        return if (chars.size < 2) text else chars.joinToString(sep)
    }
}

/** 快捷片語：打縮寫（中文、英文都可以）就跳出整段常用的話 */
class ShortcutStore(private val file: File?) {
    data class Item(val code: String, val phrase: String)

    val items = ArrayList<Item>()

    init {
        load()
    }

    fun load() {
        items.clear()
        val f = file?.takeIf { it.exists() } ?: return
        runCatching {
            val arr = JSONArray(f.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items.add(Item(o.getString("code"), o.getString("phrase")))
            }
        }
    }

    fun add(code: String, phrase: String) {
        val c = code.trim()
        if (c.isEmpty() || phrase.isEmpty()) return
        items.removeAll { it.code == c && it.phrase == phrase }
        items.add(0, Item(c, phrase))
        save()
    }

    fun remove(item: Item) {
        items.remove(item)
        save()
    }

    /** 縮寫完全一樣的片語（英文不分大小寫） */
    fun matching(text: String): List<String> {
        val t = text.trim()
        if (t.isEmpty()) return emptyList()
        return items.filter { it.code.equals(t, ignoreCase = true) }.map { it.phrase }
    }

    private fun save() {
        val f = file ?: return
        val arr = JSONArray()
        for (it in items) arr.put(JSONObject().put("code", it.code).put("phrase", it.phrase))
        f.writeText(arr.toString())
    }
}
