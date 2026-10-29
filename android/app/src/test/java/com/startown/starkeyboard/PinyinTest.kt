package com.startown.starkeyboard

import com.startown.starkeyboard.core.ChineseConverter
import com.startown.starkeyboard.core.MemoryPhraseSource
import com.startown.starkeyboard.core.PinyinEngine
import com.startown.starkeyboard.core.UserPhraseModel
import com.startown.starkeyboard.core.ZhuyinEngine
import org.junit.Assert.assertEquals
import org.junit.Test

/** 和 iPhone 版 PinyinTests 一樣的檢查（字典用沒聲調的注音當 key） */
class PinyinTest {
    private fun pinyin(): PinyinEngine {
        val rows = listOf(
            Triple("ㄋㄧ", "你", -3.0), Triple("ㄏㄠ", "好", -3.0), Triple("ㄋㄧ-ㄏㄠ", "你好", -4.0),
            Triple("ㄏㄞ", "還", -3.2), Triple("ㄏㄜ", "和", -3.0), Triple("ㄒㄧ", "西", -3.5), Triple("ㄢ", "安", -3.6),
            Triple("ㄒㄧ-ㄢ", "西安", -5.0), Triple("ㄒㄧㄢ", "先", -3.4), Triple("ㄓㄨㄥ-ㄍㄨㄛ", "中國", -3.5),
            Triple("ㄌㄩ-ㄙㄜ", "綠色", -4.5), Triple("ㄊㄞ-ㄨㄢ", "臺灣", -4.0),
        )
        val table = mapOf(
            "ni" to "ㄋㄧ", "hao" to "ㄏㄠ", "hai" to "ㄏㄞ", "he" to "ㄏㄜ", "xi" to "ㄒㄧ", "an" to "ㄢ",
            "xian" to "ㄒㄧㄢ", "zhong" to "ㄓㄨㄥ", "guo" to "ㄍㄨㄛ", "lv" to "ㄌㄩ", "se" to "ㄙㄜ",
            "tai" to "ㄊㄞ", "wan" to "ㄨㄢ",
        )
        return PinyinEngine(ZhuyinEngine(MemoryPhraseSource(rows), UserPhraseModel(null)), table)
    }

    private fun pieces(p: PinyinEngine, s: String) = p.segment(s).pieces.map { it.pinyin }

    @Test fun segmentation() {
        val p = pinyin()
        assertEquals(listOf("ni", "hao"), pieces(p, "nihao"))
        assertEquals(listOf("xian"), pieces(p, "xian"))
        assertEquals(listOf("xi", "an"), pieces(p, "xi'an"))
        assertEquals(listOf("zhong", "guo"), pieces(p, "zhongguo"))
        assertEquals(listOf("lv", "se"), pieces(p, "lvse"))
        val half = p.segment("nih")
        assertEquals(listOf("ni"), half.pieces.map { it.pinyin })
        assertEquals("h", half.partial)
        assertEquals("ni h", p.display("nih"))
    }

    @Test fun candidates() {
        val p = pinyin()
        assertEquals("你好", p.candidates("nihao").first().text)
        assertEquals("你好", p.candidates("nih").first().text)
        assertEquals("中國", p.candidates("zhongguo").first().text)
        assertEquals("臺灣", p.candidates("taiwan").first().text)
        assertEquals(2, p.consumedLength("nihao", 1))
        assertEquals(3, p.consumedLength("xi'an", 1))
        assertEquals(5, p.consumedLength("nihao", 2))
    }

    @Test fun simplified() {
        val table = mapOf("灣" to "湾", "頭髮" to "头发", "頭" to "头", "髮" to "发", "臺" to "台", "國" to "国")
        val c = ChineseConverter { table[it] }
        assertEquals("台湾", c.convert("臺灣"))
        assertEquals("头发好", c.convert("頭髮好"))
        assertEquals("中国", c.convert("中國"))
        assertEquals("你好", c.convert("你好"))
    }
}
