package com.startown.starkeyboard.core

import java.io.File

data class Candidate(val text: String, val span: Int, val kind: Kind) {
    enum class Kind { SENTENCE, PHRASE, LITERAL }
}

data class Scored(val text: String, val score: Double)

/** 字典：讀音 → 字詞（SQLite 或測試用的記憶體版本） */
interface PhraseSource {
    /** 一個讀音（例如「ㄋㄧˇ-ㄏㄠˇ」）的所有字詞，分數高的在前面 */
    fun lookup(reading: String): List<Scored>

    /** 以某個字開頭的詞（聯想詞用），分數高的在前面 */
    fun startingWith(head: String, limit: Int): List<String>

    /** 每個音節第一個注音接起來是 abbr 的所有字詞（讀音、字詞、分數），例如「ㄋㄏ」→ ㄋㄧˇ-ㄏㄠˇ 你好 */
    fun byAbbreviation(abbr: String): List<Triple<String, String, Double>>
}

class MemoryPhraseSource(rows: List<Triple<String, String, Double>>) : PhraseSource {
    private val byReading = rows.groupBy({ it.first }, { Scored(it.second, it.third) })
        .mapValues { (_, v) -> v.sortedByDescending { it.score } }
    private val all = rows.map { Scored(it.second, it.third) }
    private val byAbbr = rows.groupBy { r -> r.first.split('-').joinToString("") { it.take(1) } }

    override fun lookup(reading: String) = byReading[reading] ?: emptyList()

    override fun byAbbreviation(abbr: String) = byAbbr[abbr] ?: emptyList()

    override fun startingWith(head: String, limit: Int): List<String> =
        all.filter { it.text.length > head.length && it.text.startsWith(head) }
            .groupBy { it.text }.map { (k, v) -> Scored(k, v.maxOf { it.score }) }
            .sortedByDescending { it.score }.take(limit).map { it.text }
}

/** 注音選字：整串用動態規劃找分數最高的斷詞，也會猜打錯的字 */
class ZhuyinEngine(private val source: PhraseSource, val userModel: UserPhraseModel) {
    /** 容錯：打錯鍵、聲調錯、ㄣㄥ這類模糊音也猜得到 */
    data class FuzzyOptions(
        val keyNeighbors: Boolean = true,   // 打出來的音不存在時，試試旁邊的鍵
        val tones: Boolean = true,          // 聲調打錯或忘了打
        val phonetic: Boolean = false,      // ㄣㄥ、ㄓㄗ、ㄔㄘ、ㄕㄙ 分不清
    )

    var fuzzy = FuzzyOptions()
        set(value) {
            if (value != field) {
                field = value
                fuzzyCache.clear()
                alternativeCache.clear()
            }
        }

    private val cache = HashMap<String, List<Scored>>()
    private val fuzzyCache = HashMap<String, List<Scored>>()
    private val alternativeCache = HashMap<String, List<Pair<String, Double>>>()

    companion object {
        const val MAX_PHRASE = 6
        const val UNKNOWN_PENALTY = -30.0
        const val USER_PHRASE_BASE = -6.0
        const val TONE_PENALTY = -2.5
        const val PHONETIC_PENALTY = -1.2
        const val NEIGHBOR_PENALTY = -2.5
        const val PREFIX_PENALTY = -1.0      // 只打了開頭（ㄋ 當 ㄋㄧ），比打完整的排後面一點

        /** 畫面上顯示的注音（拿掉查字典用的 *） */
        fun display(reading: String) = reading.replace("*", "")

        /**
         * 讀音合不合 pattern：有 * 的音節只要開頭一樣（聲調不限），其他要完全一樣。
         * 合的話回傳要扣的分數，不合回傳 null
         */
        fun matchPenalty(pattern: List<String>, reading: List<String>): Double? {
            if (pattern.size != reading.size) return null
            var penalty = 0.0
            for (i in pattern.indices) {
                val want = pattern[i]
                val have = reading[i]
                if (!want.endsWith('*')) {
                    if (want != have) return null
                    continue
                }
                val head = want.dropLast(1)
                val plain = have.filter { Bopomofo.TONES.indexOf(it) < 0 }
                if (!plain.startsWith(head)) return null
                if (plain != head) penalty += PREFIX_PENALTY
            }
            return penalty
        }
    }

    /** 查字典：讀音裡有沒打完的音節（*）就用開頭注音查，再一個一個比 */
    private fun lookup(key: String): List<Scored> {
        if ('*' !in key) return source.lookup(key).take(80)
        val pattern = key.split('-')
        val best = HashMap<String, Double>()
        for ((reading, text, score) in source.byAbbreviation(pattern.joinToString("") { it.take(1) })) {
            val penalty = matchPenalty(pattern, reading.split('-')) ?: continue
            val s = score + penalty
            val old = best[text]
            if (old == null || s > old) best[text] = s
        }
        return best.entries.map { Scored(it.key, it.value) }.sortedByDescending { it.score }.take(80)
    }

    fun phrases(readings: List<String>): List<Scored> {
        val key = readings.joinToString("-")
        cache[key]?.let { return it }
        val best = LinkedHashMap<String, Double>()
        for (p in lookup(key)) {
            if (p.text !in best) best[p.text] = p.score
        }
        for (p in userModel.phrases(key)) {
            if (p !in best) best[p] = USER_PHRASE_BASE
        }
        val result = best.entries.map { Scored(it.key, it.value + userModel.boost(key, it.key)) }
            .sortedByDescending { it.score }
        cache[key] = result
        return result
    }

    /** 和 phrases 一樣，但也算進打錯的可能（每個詞最多猜錯一個音節，分數扣一點） */
    fun fuzzyPhrases(readings: List<String>): List<Scored> {
        val key = readings.joinToString("-")
        fuzzyCache[key]?.let { return it }
        val best = LinkedHashMap<String, Double>()
        fun merge(list: List<Scored>, penalty: Double) {
            for (p in list) {
                val s = p.score + penalty
                val old = best[p.text]
                if (old == null || s > old) best[p.text] = s
            }
        }
        merge(phrases(readings), 0.0)
        for (j in readings.indices) {
            for ((alt, penalty) in alternatives(readings[j])) {
                val variant = readings.toMutableList()
                variant[j] = alt
                merge(phrases(variant), penalty)
            }
        }
        val result = best.entries.map { Scored(it.key, it.value) }.sortedByDescending { it.score }
        fuzzyCache[key] = result
        return result
    }

    /** 一個音節可能是哪些音打錯了（只留字典裡真的有的） */
    fun alternatives(reading: String): List<Pair<String, Double>> {
        alternativeCache[reading]?.let { return it }
        if (reading.endsWith('*')) return emptyList()   // 沒打完的音節本來就聲調不限，不用再猜
        val syllable = Syllable.parse(reading)
        val out = ArrayList<Pair<String, Double>>()
        val seen = hashSetOf(reading)
        fun add(s: Syllable, penalty: Double) {
            val r = s.reading
            if (!s.hasSound || !seen.add(r)) return
            if (phrases(listOf(r)).isNotEmpty()) out.add(r to penalty)
        }
        if (fuzzy.tones) {
            for (tone in listOf(null, 'ˊ', 'ˇ', 'ˋ', '˙')) {
                if (tone != syllable.tone) add(syllable.copy(tone = tone), TONE_PENALTY)
            }
        }
        if (fuzzy.phonetic) {
            syllable.initial?.let { i -> Bopomofo.similar[i]?.let { add(syllable.copy(initial = it), PHONETIC_PENALTY) } }
            syllable.rhyme?.let { f -> Bopomofo.similar[f]?.let { add(syllable.copy(rhyme = it), PHONETIC_PENALTY) } }
        }
        // 打出來的音根本不存在：多半是按到旁邊的鍵
        if (fuzzy.keyNeighbors && phrases(listOf(reading)).isEmpty()) {
            for (c in reading) {
                for (n in Bopomofo.neighbors[c] ?: emptyList()) {
                    val s = syllable.copy()
                    when (Bopomofo.kind(c)) {
                        BopomofoKind.INITIAL -> s.initial = null
                        BopomofoKind.MEDIAL -> s.medial = null
                        BopomofoKind.RHYME -> s.rhyme = null
                        BopomofoKind.TONE -> s.tone = null
                        null -> continue
                    }
                    s.put(n)
                    add(s, NEIGHBOR_PENALTY)
                }
            }
        }
        alternativeCache[reading] = out
        return out
    }

    /** 整串注音分數最高的斷法 */
    fun bestSegments(readings: List<String>): List<Scored> {
        val n = readings.size
        if (n == 0) return emptyList()
        val best = DoubleArray(n + 1) { Double.NEGATIVE_INFINITY }
        val backStart = IntArray(n + 1)
        val backText = arrayOfNulls<String>(n + 1)
        val backScore = DoubleArray(n + 1)
        best[0] = 0.0
        for (i in 1..n) {
            for (k in 1..minOf(MAX_PHRASE, i)) {
                val start = i - k
                if (best[start] == Double.NEGATIVE_INFINITY) continue
                val top = fuzzyPhrases(readings.subList(start, i)).firstOrNull()
                if (top != null) {
                    val s = best[start] + top.score
                    if (s > best[i]) {
                        best[i] = s; backStart[i] = start; backText[i] = top.text; backScore[i] = top.score
                    }
                } else if (k == 1) {
                    val s = best[start] + UNKNOWN_PENALTY
                    if (s > best[i]) {
                        best[i] = s; backStart[i] = start; backText[i] = display(readings[start]); backScore[i] = UNKNOWN_PENALTY
                    }
                }
            }
        }
        val segments = ArrayList<Scored>()
        var i = n
        while (i > 0) {
            segments.add(Scored(backText[i] ?: "", backScore[i]))
            i = backStart[i]
        }
        return segments.reversed()
    }

    /** 候選字：第一個是整句，接著是從開頭算起最長到最短的詞 */
    fun candidates(readings: List<String>): List<Candidate> {
        if (readings.isEmpty()) return emptyList()
        val out = ArrayList<Candidate>()
        val seen = HashSet<String>()
        if (readings.size > 1) {
            val sentence = bestSegments(readings).joinToString("") { it.text }
            out.add(Candidate(sentence, readings.size, Candidate.Kind.SENTENCE))
            seen.add(sentence)
        }
        var hasSingle = false
        for (k in minOf(MAX_PHRASE, readings.size) downTo 1) {
            for (p in fuzzyPhrases(readings.subList(0, k))) {
                if (!seen.add(p.text)) continue
                out.add(Candidate(p.text, k, Candidate.Kind.PHRASE))
                if (k == 1) hasSingle = true
            }
        }
        if (!hasSingle) out.add(Candidate(display(readings[0]), 1, Candidate.Kind.LITERAL))
        return out
    }

    /** 聯想詞：打完一個字後，接在它後面常見的字詞 */
    fun associations(after: String, limit: Int = 30): List<String> {
        if (after.isEmpty()) return emptyList()
        val head = after.substring(after.offsetByCodePoints(after.length, -1))
        return source.startingWith(head, limit).map { it.substring(head.length) }.filter { it.isNotEmpty() }
    }

    fun learn(readings: List<String>, text: String) {
        if (readings.isEmpty() || readings.size > MAX_PHRASE) return
        userModel.record(readings.joinToString("-"), text)
        cache.clear()
        fuzzyCache.clear()
    }

    fun resetLearning() {
        userModel.reset()
        cache.clear()
        fuzzyCache.clear()
    }
}

/** 記住使用者選過的字詞（每行：讀音 TAB 字詞 TAB 次數） */
class UserPhraseModel(private val file: File?) {
    private val counts = HashMap<String, HashMap<String, Int>>()

    init {
        file?.takeIf { it.exists() }?.forEachLine { line ->
            val parts = line.split('\t')
            if (parts.size == 3) {
                parts[2].toIntOrNull()?.let { counts.getOrPut(parts[0]) { HashMap() }[parts[1]] = it }
            }
        }
    }

    /** 每選一次加 0.8（log10，大約變成 6 倍常用），最多算 5 次 */
    fun boost(reading: String, phrase: String): Double {
        val c = counts[reading]?.get(phrase) ?: return 0.0
        return minOf(c, 5) * 0.8
    }

    fun phrases(reading: String): List<String> = counts[reading]?.keys?.sorted() ?: emptyList()

    fun record(reading: String, phrase: String) {
        val m = counts.getOrPut(reading) { HashMap() }
        m[phrase] = (m[phrase] ?: 0) + 1
        save()
    }

    fun reset() {
        counts.clear()
        save()
    }

    private fun save() {
        val f = file ?: return
        f.writeText(buildString {
            for ((r, m) in counts) for ((p, c) in m) append(r).append('\t').append(p).append('\t').append(c).append('\n')
        })
    }
}
