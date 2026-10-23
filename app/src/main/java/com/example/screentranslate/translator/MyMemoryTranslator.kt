package com.example.screentranslate.translator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

/**
 * 默认翻译实现：MyMemory 公共翻译 API。
 * 优点：完全免费、无需注册、无需 API Key，开箱即用。
 * 缺点：匿名额度约 5000 词/天。
 *
 * 源语言默认面向「日文」，并内置自动探测：
 *   · 含日文假名 → 按日文(ja)翻；
 *   · 只含汉字、无假名 → 判为中文(zh)，直接返回原文、不调接口、省额度；
 *   · 其余 → 按英文(en)兜底。
 * 这样英日混杂的界面也能各自翻对，不用手改。
 *
 * 想换更稳的引擎（LibreTranslate、有道、百度等），
 * 新建一个实现 Translator 接口的类，然后在 TranslateService 里换掉即可。
 */
class MyMemoryTranslator : Translator {

    private val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .build()

    override suspend fun translate(text: String): String = withContext(Dispatchers.IO) {
        try {
            val source = detectSource(text)
            // 已经是中文就不翻，直接返回原文，省额度也省时间
            if (source == "zh") return@withContext text

            val q = URLEncoder.encode(text, "UTF-8")
            val url = "https://api.mymemory.translated.net/get?q=$q&langpair=$source|zh-CN"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext "翻译失败(网络 ${response.code})"
            }
            val body = response.body?.string().orEmpty()
            val json = JSONObject(body)
            val status = json.optString("responseStatus")
            if (status != "200") {
                return@withContext "翻译失败($status)"
            }
            json.getJSONObject("responseData").getString("translatedText")
        } catch (e: Exception) {
            "翻译出错：${e.message}"
        }
    }

    /**
     * 粗略判断来源语言（本项目默认面向日文）：
     *  含日文假名(ひらがな/カタカナ) → "ja"
     *  只含汉字、无假名               → "zh"（已是中文）
     *  其余                           → "en"（兜底）
     */
    private fun detectSource(text: String): String {
        var kana = 0
        var han = 0
        for (ch in text) {
            val code = ch.code
            if (code in 0x3040..0x309F || code in 0x30A0..0x30FF) {
                kana++
            } else if (code in 0x4E00..0x9FFF) {
                han++
            }
        }
        return when {
            kana > 0 -> "ja"
            han > 0 -> "zh"
            else -> "en"
        }
    }
}
