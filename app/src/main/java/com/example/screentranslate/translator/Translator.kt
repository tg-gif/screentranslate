package com.example.screentranslate.translator

/**
 * 翻译引擎接口。所有翻译实现都遵循这个约定，
 * 这样后面想换成 LibreTranslate / 有道 / 百度 时，只改这一处即可。
 */
interface Translator {
    /**
     * 把文字翻译成中文（目标语言固定 zh-CN）。
     * 必须在协程中调用（内部已切到 IO 线程）。
     */
    suspend fun translate(text: String): String
}
