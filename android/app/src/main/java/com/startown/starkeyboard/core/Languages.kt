package com.startown.starkeyboard.core

/** 韓文兩排式（두벌식）組字：子音、母音照規則自動拼成一個字 */
class HangulComposer {
    data class Output(val commit: String, val composing: String)

    companion object {
        const val CHOSEONG = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
        const val JUNGSEONG = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
        const val JONGSEONG = "ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"   // 前面還有一個「沒有收尾」

        val compoundVowels = mapOf(
            "ㅗㅏ" to 'ㅘ', "ㅗㅐ" to 'ㅙ', "ㅗㅣ" to 'ㅚ', "ㅜㅓ" to 'ㅝ', "ㅜㅔ" to 'ㅞ', "ㅜㅣ" to 'ㅟ', "ㅡㅣ" to 'ㅢ',
        )
        val compoundFinals = mapOf(
            "ㄱㅅ" to 'ㄳ', "ㄴㅈ" to 'ㄵ', "ㄴㅎ" to 'ㄶ', "ㄹㄱ" to 'ㄺ', "ㄹㅁ" to 'ㄻ', "ㄹㅂ" to 'ㄼ',
            "ㄹㅅ" to 'ㄽ', "ㄹㅌ" to 'ㄾ', "ㄹㅍ" to 'ㄿ', "ㄹㅎ" to 'ㅀ', "ㅂㅅ" to 'ㅄ',
        )

        fun isConsonant(c: Char) = CHOSEONG.indexOf(c) >= 0
        fun isVowel(c: Char) = JUNGSEONG.indexOf(c) >= 0
    }

    private var cho: Char? = null
    private var v1: Char? = null
    private var v2: Char? = null
    private var j1: Char? = null
    private var j2: Char? = null

    val isEmpty get() = cho == null && v1 == null

    private val vowel: Char?
        get() {
            val a = v1 ?: return null
            val b = v2 ?: return a
            return compoundVowels["$a$b"]
        }

    private val finalConsonant: Char?
        get() {
            val a = j1 ?: return null
            val b = j2 ?: return a
            return compoundFinals["$a$b"]
        }

    val composing: String
        get() {
            val c = cho
            val v = vowel
            if (c != null && v != null) {
                val ci = CHOSEONG.indexOf(c)
                val vi = JUNGSEONG.indexOf(v)
                val ji = finalConsonant?.let { JONGSEONG.indexOf(it) + 1 } ?: 0
                return (0xAC00 + (ci * 21 + vi) * 28 + ji).toChar().toString()
            }
            return c?.toString() ?: v?.toString() ?: ""
        }

    fun input(c: Char): Output = when {
        isConsonant(c) -> consonant(c)
        isVowel(c) -> vowelInput(c)
        else -> {
            val done = composing
            reset()
            Output(done + c, "")
        }
    }

    private fun startOver(newCho: Char? = null, newVowel: Char? = null): Output {
        val done = composing
        reset()
        cho = newCho
        v1 = newVowel
        return Output(done, composing)
    }

    private fun consonant(c: Char): Output {
        if (v1 == null) return startOver(newCho = c)
        if (cho != null) {
            if (j1 == null && JONGSEONG.indexOf(c) >= 0) {
                j1 = c
                return Output("", composing)
            }
            val j = j1
            if (j != null && j2 == null && compoundFinals["$j$c"] != null) {
                j2 = c
                return Output("", composing)
            }
        }
        return startOver(newCho = c)
    }

    private fun vowelInput(c: Char): Output {
        if (v1 == null) {
            v1 = c
            return Output("", composing)
        }
        if (j1 == null) {
            val first = v1
            if (v2 == null && first != null && compoundVowels["$first$c"] != null) {
                v2 = c
                return Output("", composing)
            }
            return startOver(newVowel = c)
        }
        // 有收尾子音時打母音：最後一個子音搬到下一個字當開頭（닭 + ㅏ → 달가）
        val moving: Char
        if (j2 != null) {
            moving = j2!!
            j2 = null
        } else {
            moving = j1!!
            j1 = null
        }
        return startOver(newCho = moving, newVowel = c)
    }

    fun backspace(): Boolean {
        when {
            j2 != null -> j2 = null
            j1 != null -> j1 = null
            v2 != null -> v2 = null
            v1 != null -> v1 = null
            cho != null -> cho = null
            else -> return false
        }
        return true
    }

    fun reset() {
        cho = null; v1 = null; v2 = null; j1 = null; j2 = null
    }
}

/** 日文羅馬拼音轉假名（sakura → さくら → サクラ） */
object Romaji {
    val table: Map<String, String> by lazy { build() }

    /** 還沒打完的拼音（k、ky、sh、ts…） */
    private val prefixes: Set<String> by lazy {
        val set = HashSet<String>()
        for (key in table.keys) for (len in 1 until key.length) set.add(key.substring(0, len))
        set.add("n")
        set
    }

    private fun build(): Map<String, String> {
        val t = HashMap<String, String>()
        val vowels = listOf("a", "i", "u", "e", "o")
        fun row(head: String, kana: String) {
            for ((i, v) in vowels.withIndex()) t[head + v] = kana[i].toString()
        }
        row("", "あいうえお")
        row("k", "かきくけこ"); row("s", "さしすせそ"); row("t", "たちつてと")
        row("n", "なにぬねの"); row("h", "はひふへほ"); row("m", "まみむめも")
        row("r", "らりるれろ"); row("g", "がぎぐげご"); row("z", "ざじずぜぞ")
        row("d", "だぢづでど"); row("b", "ばびぶべぼ"); row("p", "ぱぴぷぺぽ")
        row("x", "ぁぃぅぇぉ"); row("l", "ぁぃぅぇぉ")
        t["ya"] = "や"; t["yu"] = "ゆ"; t["yo"] = "よ"; t["ye"] = "いぇ"
        t["wa"] = "わ"; t["wo"] = "を"; t["wi"] = "うぃ"; t["we"] = "うぇ"
        t["shi"] = "し"; t["chi"] = "ち"; t["tsu"] = "つ"; t["fu"] = "ふ"; t["ji"] = "じ"
        val youon = listOf(
            "ky" to "き", "sh" to "し", "sy" to "し", "ch" to "ち", "ty" to "ち", "cy" to "ち",
            "ny" to "に", "hy" to "ひ", "my" to "み", "ry" to "り", "gy" to "ぎ", "j" to "じ",
            "jy" to "じ", "zy" to "じ", "by" to "び", "py" to "ぴ", "dy" to "ぢ",
        )
        for ((head, kana) in youon) {
            t[head + "a"] = kana + "ゃ"; t[head + "u"] = kana + "ゅ"; t[head + "o"] = kana + "ょ"; t[head + "e"] = kana + "ぇ"
        }
        t["fa"] = "ふぁ"; t["fi"] = "ふぃ"; t["fe"] = "ふぇ"; t["fo"] = "ふぉ"
        t["va"] = "ゔぁ"; t["vi"] = "ゔぃ"; t["vu"] = "ゔ"; t["ve"] = "ゔぇ"; t["vo"] = "ゔぉ"
        t["tsa"] = "つぁ"; t["thi"] = "てぃ"; t["dhi"] = "でぃ"; t["dhu"] = "でゅ"
        t["xtu"] = "っ"; t["ltu"] = "っ"; t["xtsu"] = "っ"; t["ltsu"] = "っ"
        t["xya"] = "ゃ"; t["xyu"] = "ゅ"; t["xyo"] = "ょ"; t["lya"] = "ゃ"; t["lyu"] = "ゅ"; t["lyo"] = "ょ"
        t["xwa"] = "ゎ"; t["xn"] = "ん"; t["n'"] = "ん"
        t["-"] = "ー"; t[","] = "、"; t["."] = "。"; t["?"] = "？"; t["!"] = "！"
        return t
    }

    /** 轉成假名；最後還沒打完的拼音放在 second */
    fun convert(input: String): Pair<String, String> {
        val s = input.lowercase()
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            var matched = false
            for (len in minOf(4, s.length - i) downTo 1) {
                val kana = table[s.substring(i, i + len)]
                if (kana != null) {
                    out.append(kana)
                    i += len
                    matched = true
                    break
                }
            }
            if (matched) continue
            val c = s[i]
            val next = s.getOrNull(i + 1)
            if (c == 'n' && next != null) {
                if (next == 'n') {
                    // nn：後面接母音或 y 時（konnichiha），第二個 n 留給下一個字
                    val after = s.getOrNull(i + 2)
                    out.append("ん")
                    i += if (after != null && "aiueoy".indexOf(after) >= 0) 1 else 2
                    continue
                }
                if ("aiueoy".indexOf(next) < 0) {
                    out.append("ん")
                    i += 1
                    continue
                }
            }
            // 促音：kka → っか
            if (next != null && next == c && c.isLetter() && "aiueon".indexOf(c) < 0) {
                out.append("っ")
                i += 1
                continue
            }
            val rest = s.substring(i)
            if (rest in prefixes) return out.toString() to rest
            out.append(c)
            i += 1
        }
        return out.toString() to ""
    }

    /** 送出時：最後單獨的 n 當成ん，其他沒打完的照原樣 */
    fun finalize(input: String): String {
        val (kana, rest) = convert(input)
        return kana + if (rest == "n") "ん" else rest
    }

    fun katakana(hiragana: String) = buildString {
        for (ch in hiragana) append(if (ch in 'ぁ'..'ゖ') (ch + 0x60) else ch)
    }
}

/** 英文自動修正用的小工具（真正查字典的是 Android 的拼字檢查） */
object Spelling {
    /** 兩個字差幾個字母（插入、刪除、替換、相鄰兩個字母對調都算一步） */
    fun editDistance(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + 1)
                }
            }
        }
        return d[a.length][b.length]
    }

    /** 修正要夠接近才自動換（短字只能差 1 個字母） */
    fun isCloseEnough(typed: String, fix: String): Boolean {
        val limit = if (typed.length <= 4) 1 else 2
        return editDistance(typed.lowercase(), fix.lowercase()) <= limit
    }

    /** 照原本的大小寫：Hello → 修正也開頭大寫 */
    fun matchCase(fix: String, typed: String): String {
        val first = typed.firstOrNull() ?: return fix
        if (!first.isUpperCase() || fix.isEmpty()) return fix
        return fix.substring(0, 1).uppercase() + fix.substring(1)
    }
}
