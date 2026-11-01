package com.startown.starkeyboard.core

/** 符號頁要送出的字。程式／Markdown 用的符號一律半形，中文標點維持全形。和 iOS SymbolPages 同一套字。 */
object SymbolPages {
    const val MARKDOWN = "`*_~|#\\^+=[]{}<>%"
    const val ZHUYIN_SYMBOL_ROW1 = "`*_~|#\\^+="
    const val ZHUYIN_SYMBOL_ROW2 = "{[]<>%}～—…"
    const val ZHUYIN_NUMBER_ROW2 = "-/：；（）$@「」"
    const val ZHUYIN_NUMBER_ROW3 = "。，、？！."
    /** 切成「半形」時：括號、冒號、分號、問號、驚嘆號改用半形 */
    const val ZHUYIN_NUMBER_ROW2_HALF = "-/:;()$@「」"
    const val ZHUYIN_NUMBER_ROW3_HALF = "。，、?!."
    const val ENGLISH_SYMBOL_ROW1 = "[]{}#%^*+="
    const val ENGLISH_SYMBOL_ROW2 = "_\\|~<>€£¥`"

    fun isAscii(c: Char) = c.code < 128
}
