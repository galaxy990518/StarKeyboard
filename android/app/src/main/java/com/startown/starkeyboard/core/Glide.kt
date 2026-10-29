package com.startown.starkeyboard.core

import kotlin.math.hypot
import kotlin.math.log10

/** 平面上的一點（不用 Android 的 PointF，核心邏輯才能在電腦上測試） */
data class Pt(val x: Float, val y: Float)

/**
 * 英文滑行輸入：手指不離開，滑過字母，認出是哪個字。和 iPhone 版 Glide.swift 一樣。
 *
 * 做法：先用起點、終點附近的鍵挑出可能的字，再把每個字「理想的滑法」（依序連起字母鍵的中心）
 * 和手指實際畫的線各取 24 個等距的點比距離；距離越近、字越常用，分數越高。
 */
class GlideDecoder(words: List<Pair<String, Int>>) {
    private class Entry(val word: String, val squeezed: String, val logFreq: Double)

    companion object {
        const val SAMPLES = 24
        const val DISTANCE_WEIGHT = 6.0
        const val MAX_DISTANCE = 1.4   // 平均差超過 1.4 個鍵寬就不考慮

        /** 連續重複的字母只算一次（hello → helo），滑的時候不會在同一個鍵停兩次 */
        fun squeeze(w: String) = buildString { for (c in w) if (isEmpty() || last() != c) append(c) }

        /** 沿著線等距取 n 個點 */
        fun resample(points: List<Pt>, n: Int): List<Pt> {
            if (points.size == 1) return List(n) { points[0] }
            val seg = DoubleArray(points.size - 1) { i -> hypot((points[i + 1].x - points[i].x).toDouble(), (points[i + 1].y - points[i].y).toDouble()) }
            val total = seg.sum()
            if (total <= 0.0) return List(n) { points[0] }
            val out = ArrayList<Pt>(n)
            var j = 0
            var walked = 0.0
            for (k in 0 until n) {
                val target = total * k / (n - 1)
                while (j < seg.size - 1 && walked + seg[j] < target) { walked += seg[j]; j++ }
                val t = if (seg[j] > 0) ((target - walked) / seg[j]).coerceIn(0.0, 1.0) else 0.0
                val a = points[j]
                val b = points[j + 1]
                out.add(Pt((a.x + (b.x - a.x) * t).toFloat(), (a.y + (b.y - a.y) * t).toFloat()))
            }
            return out
        }
    }

    /** 依「第一個字母＋最後一個字母」分組，查的時候只看起點終點附近的組 */
    private val byEnds: Map<String, List<Entry>> = words
        .filter { (w, _) -> w.length >= 2 && w.all { it in 'a'..'z' } }
        .map { (w, n) -> Entry(w, squeeze(w), log10(maxOf(n, 1).toDouble())) }
        .groupBy { "" + it.word.first() + it.word.last() }

    val size get() = byEnds.values.sumOf { it.size }

    /** path：手指畫的線；centers：每個字母鍵的中心；keyWidth：一個鍵多寬。回傳最可能的幾個字 */
    fun decode(path: List<Pt>, centers: Map<Char, Pt>, keyWidth: Float, limit: Int = 5): List<String> {
        if (path.size < 2 || centers.isEmpty() || keyWidth <= 0f) return emptyList()
        fun nearest(p: Pt) = centers.entries.sortedBy { hypot(it.value.x - p.x, it.value.y - p.y) }.take(2).map { it.key }
        val starts = nearest(path.first())
        val ends = nearest(path.last())
        val user = resample(path, SAMPLES)
        val scored = ArrayList<Pair<String, Double>>()
        for (s in starts) for (e in ends) {
            for (entry in byEnds["$s$e"] ?: continue) {
                val ideal = entry.squeezed.map { centers[it] ?: return@map null }
                if (ideal.any { it == null }) continue
                val shape = resample(ideal.filterNotNull(), SAMPLES)
                var d = 0.0
                for (k in 0 until SAMPLES) d += hypot((user[k].x - shape[k].x).toDouble(), (user[k].y - shape[k].y).toDouble())
                val dist = d / SAMPLES / keyWidth
                if (dist > MAX_DISTANCE) continue
                scored.add(entry.word to (entry.logFreq - dist * DISTANCE_WEIGHT))
            }
        }
        return scored.sortedByDescending { it.second }.map { it.first }.distinct().take(limit)
    }
}
