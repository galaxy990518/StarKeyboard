package com.startown.starkeyboard.core

enum class BopomofoKind { INITIAL, MEDIAL, RHYME, TONE }

object Bopomofo {
    const val INITIALS = "ㄅㄆㄇㄈㄉㄊㄋㄌㄍㄎㄏㄐㄑㄒㄓㄔㄕㄖㄗㄘㄙ"
    const val MEDIALS = "ㄧㄨㄩ"
    const val RHYMES = "ㄚㄛㄜㄝㄞㄟㄠㄡㄢㄣㄤㄥㄦ"
    const val TONES = "ˊˇˋ˙"

    fun kind(c: Char): BopomofoKind? = when {
        INITIALS.indexOf(c) >= 0 -> BopomofoKind.INITIAL
        MEDIALS.indexOf(c) >= 0 -> BopomofoKind.MEDIAL
        RHYMES.indexOf(c) >= 0 -> BopomofoKind.RHYME
        TONES.indexOf(c) >= 0 -> BopomofoKind.TONE
        else -> null
    }

    /** 大千排列：和電腦鍵盤的 1qaz、2wsx… 直排一樣 */
    val dachenRows = listOf(
        "ㄅㄉˇˋㄓˊ˙ㄚㄞㄢㄦ",
        "ㄆㄊㄍㄐㄔㄗㄧㄛㄟㄣ",
        "ㄇㄋㄎㄑㄕㄘㄨㄜㄠㄤ",
        "ㄈㄌㄏㄒㄖㄙㄩㄝㄡㄥ",
    )

    /** 常分不清的音（模糊音） */
    val similar = mapOf(
        'ㄣ' to 'ㄥ', 'ㄥ' to 'ㄣ', 'ㄓ' to 'ㄗ', 'ㄗ' to 'ㄓ',
        'ㄔ' to 'ㄘ', 'ㄘ' to 'ㄔ', 'ㄕ' to 'ㄙ', 'ㄙ' to 'ㄕ',
    )

    /** 鍵盤上相鄰的鍵（左右、上下） */
    val neighbors: Map<Char, List<Char>> by lazy {
        val map = HashMap<Char, List<Char>>()
        for ((r, row) in dachenRows.withIndex()) {
            for ((c, ch) in row.withIndex()) {
                val list = ArrayList<Char>()
                for ((dr, dc) in listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0)) {
                    val rr = r + dr
                    val cc = c + dc
                    if (rr in dachenRows.indices && cc >= 0 && cc < dachenRows[rr].length) {
                        list.add(dachenRows[rr][cc])
                    }
                }
                map[ch] = list
            }
        }
        map
    }
}

/** 一個字的注音：聲母＋介音＋韻母＋聲調（一聲不標） */
data class Syllable(
    var initial: Char? = null,
    var medial: Char? = null,
    var rhyme: Char? = null,
    var tone: Char? = null,
) {
    val isEmpty get() = initial == null && medial == null && rhyme == null && tone == null
    val hasSound get() = initial != null || medial != null || rhyme != null

    /** 和字典一樣的寫法，例如「ㄋㄧˇ」「ㄇㄚ」「ㄉㄜ˙」 */
    val reading: String
        get() = buildString { listOf(initial, medial, rhyme, tone).forEach { if (it != null) append(it) } }

    /** 大千的打法：同一類的符號再打一次就取代 */
    fun put(c: Char) {
        when (Bopomofo.kind(c)) {
            BopomofoKind.INITIAL -> initial = c
            BopomofoKind.MEDIAL -> medial = c
            BopomofoKind.RHYME -> rhyme = c
            BopomofoKind.TONE -> tone = c
            null -> {}
        }
    }

    fun removeLast() {
        when {
            tone != null -> tone = null
            rhyme != null -> rhyme = null
            medial != null -> medial = null
            else -> initial = null
        }
    }

    companion object {
        fun parse(reading: String) = Syllable().also { s -> reading.forEach { s.put(it) } }
    }
}

/** 組字區：已經打完的音節＋正在打的音節 */
class ZhuyinComposer {
    val syllables = ArrayList<Syllable>()
    var current = Syllable()
        private set

    val readings get() = syllables.map { it.reading }
    val isComposing get() = syllables.isNotEmpty() || !current.isEmpty
    val hasPendingSyllable get() = !current.isEmpty

    /** 回傳 false 表示這個符號沒被組字用掉（例如還沒打音就先打聲調） */
    fun input(c: Char): Boolean {
        val kind = Bopomofo.kind(c) ?: return false
        if (kind == BopomofoKind.TONE) {
            if (!current.hasSound) return false
            current.tone = c
            finish()
            return true
        }
        current.put(c)
        return true
    }

    /** 空白鍵＝一聲 */
    fun confirmSyllable(): Boolean {
        if (!current.hasSound) return false
        finish()
        return true
    }

    fun backspace(): Boolean {
        if (!current.isEmpty) {
            current.removeLast()
            return true
        }
        if (syllables.isNotEmpty()) {
            syllables.removeAt(syllables.size - 1)
            return true
        }
        return false
    }

    fun removeFirst(n: Int) {
        repeat(minOf(maxOf(n, 0), syllables.size)) { syllables.removeAt(0) }
    }

    fun clear() {
        syllables.clear()
        current = Syllable()
    }

    private fun finish() {
        syllables.add(current)
        current = Syllable()
    }
}
