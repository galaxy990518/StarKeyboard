package com.startown.starkeyboard.core

/** 漢語拼音輸入（馬來西亞、中國用）：把字母斷成音節，換成沒聲調的注音，再用同一份字典選字（和 iPhone 版 Pinyin.swift 一樣） */
class PinyinEngine(val engine: ZhuyinEngine, private val table: Map<String, String>) {
    /** 一個斷好的音節：拼音、對應的注音、在輸入裡用掉幾個字元（含 '） */
    data class Piece(val pinyin: String, val bopomofo: String, val length: Int)

    data class Segmentation(val pieces: List<Piece>, val partial: String)

    companion object {
        const val MAX_SYLLABLE = 6
    }

    /** 拼音開頭 → 所有以它開頭的音節 */
    private val completions: Map<String, List<String>> = HashMap<String, MutableList<String>>().also { map ->
        for (py in table.keys) for (len in 1 until py.length) map.getOrPut(py.substring(0, len)) { ArrayList() }.add(py)
    }.mapValues { it.value.sorted() }

    init {
        engine.fuzzy = ZhuyinEngine.FuzzyOptions(keyNeighbors = false, tones = false, phonetic = false)
    }

    fun segment(input: String): Segmentation {
        val chunks = input.lowercase().split('\'')
        val pieces = ArrayList<Piece>()
        var partial = ""
        var pending = 0   // 還沒算進去的 '
        for ((i, chunk) in chunks.withIndex()) {
            val isLast = i == chunks.size - 1
            val (found, rest) = split(chunk)
            val syllables = found.toMutableList()
            var tail = rest
            if (!isLast && tail.isNotEmpty()) {
                // 中間的段落打不完整：當成一個查不到的音節
                syllables.add(tail)
                tail = ""
            }
            for ((j, py) in syllables.withIndex()) {
                var len = py.length + pending
                pending = 0
                if (!isLast && j == syllables.size - 1) len += 1
                pieces.add(Piece(py, table[py] ?: "", len))
            }
            if (!isLast && syllables.isEmpty()) pending += 1
            if (isLast) partial = tail
        }
        return Segmentation(pieces, partial)
    }

    /** 盡量全部斷成音節，斷不完的尾巴（某些音節的開頭）放在 second */
    private fun split(s: String): Pair<List<String>, String> {
        for (cut in s.length downTo 0) {
            val tail = s.substring(cut)
            if (tail.isNotEmpty() && tail !in completions) continue
            splitFull(s.substring(0, cut))?.let { return it to tail }
        }
        return emptyList<String>() to s
    }

    /** 取最長的合法音節，後面斷不下去就往回退 */
    private fun splitFull(s: String): List<String>? {
        val memo = HashMap<Int, List<String>?>()
        fun go(start: Int): List<String>? {
            if (start == s.length) return emptyList()
            if (memo.containsKey(start)) return memo[start]
            var result: List<String>? = null
            for (len in minOf(MAX_SYLLABLE, s.length - start) downTo 1) {
                val py = s.substring(start, start + len)
                if (py !in table) continue
                val rest = go(start + len) ?: continue
                result = listOf(py) + rest
                break
            }
            memo[start] = result
            return result
        }
        return go(0)
    }

    /** 拿去查字典的注音（打到一半的音節換成分數最高的補完） */
    fun readings(seg: Segmentation): List<String> {
        val base = seg.pieces.map { it.bopomofo.ifEmpty { table[it.pinyin] ?: it.pinyin } }
        val options = completions[seg.partial]
        if (seg.partial.isEmpty() || options == null) return base
        var best: Pair<Double, String>? = null
        for (py in options) {
            val bpmf = table[py] ?: continue
            val score = engine.bestSegments(base + bpmf).sumOf { it.score }
            if (best == null || score > best.first) best = score to bpmf
        }
        return if (best != null) base + best.second else base
    }

    fun candidates(input: String) = engine.candidates(readings(segment(input)))

    /** 選了用掉前 span 個音節的候選字後，輸入裡要拿掉幾個字元 */
    fun consumedLength(input: String, span: Int): Int {
        val seg = segment(input)
        if (span >= seg.pieces.size) return input.length
        return seg.pieces.take(span).sumOf { it.length }
    }

    /** 上面那條顯示的拼音，例如「ni hao」 */
    fun display(input: String): String {
        val seg = segment(input)
        return (seg.pieces.map { it.pinyin } + listOfNotNull(seg.partial.ifEmpty { null })).joinToString(" ")
    }
}

/** 繁體轉簡體（OpenCC 的對照表，字典裡的 t2s 表） */
class ChineseConverter(private val lookup: (String) -> String?) {
    private val cache = HashMap<String, String>()

    companion object {
        const val MAX_PHRASE = 6
    }

    /** 從左邊找最長有對照的詞，沒有就逐字轉 */
    fun convert(text: String): String = cache.getOrPut(text) {
        val out = StringBuilder()
        var i = 0
        while (i < text.length) {
            var matched = false
            for (len in minOf(MAX_PHRASE, text.length - i) downTo 1) {
                val simp = lookup(text.substring(i, i + len))
                if (simp != null) {
                    out.append(simp)
                    i += len
                    matched = true
                    break
                }
            }
            if (!matched) {
                out.append(text[i])
                i += 1
            }
        }
        out.toString()
    }
}
