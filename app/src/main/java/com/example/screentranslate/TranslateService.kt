package com.example.screentranslate

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.screentranslate.translator.MyMemoryTranslator
import com.example.screentranslate.translator.Translator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 核心：无障碍服务。
 * 系统会在你开启该服务后启动它，它能读取当前屏幕上任意 App 的视图文字。
 *
 * 两个能力：
 *  1) 整页翻译：遍历当前窗口所有节点，收集文字 → 逐段翻译 → 在悬浮面板展示。
 *  2) 取词翻译：开启取词模式后，点击屏幕上「可点击/可长按」的文字即翻译该段。
 */
class TranslateService : AccessibilityService(), FloatWindow.Callback {

    private lateinit var floatWindow: FloatWindow
    private val translator: Translator = MyMemoryTranslator()
    private var pickMode = false
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        floatWindow = FloatWindow(this, this)
        // 没有悬浮窗权限就引导用户去开，开不了就不显示球
        if (floatWindow.ensureOverlayPermission()) {
            floatWindow.show()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // 确保能拿到「当前交互窗口」的完整视图层级
        val info = serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!pickMode) return
        // 取词模式：捕捉被点击 / 长按的节点文字
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED
        ) {
            val source = event.source ?: return
            val text = source.text?.toString()
            if (!text.isNullOrBlank()) {
                translateAndShow(text)
                pickMode = false
                floatWindow.setPickMode(false)
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        floatWindow.hide()
        super.onDestroy()
    }

    // ---------- FloatWindow.Callback 实现 ----------

    override fun onTogglePickMode() {
        pickMode = !pickMode
        floatWindow.setPickMode(pickMode)
    }

    override fun onTranslateScreen() {
        val root = rootInActiveWindow ?: run {
            floatWindow.showResult("无法读取当前界面，请确认无障碍服务已开启。")
            return
        }
        val texts = collectTexts(root)
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length <= 200 }
            .distinct()

        if (texts.isEmpty()) {
            floatWindow.showResult("没有检测到可读文字。")
            return
        }

        floatWindow.showResult("正在翻译 ${texts.size} 段文字……")
        scope.launch {
            val sb = StringBuilder()
            texts.forEachIndexed { index, original ->
                val translated = runCatching { translator.translate(original) }
                    .getOrDefault(original)
                sb.append("• $original\n  → $translated\n\n")
                // 每翻译 5 段刷新一次，避免长时间白屏
                if (index % 5 == 0) floatWindow.showResult(sb.toString())
            }
            floatWindow.showResult(sb.toString())
        }
    }

    // ---------- 内部方法 ----------

    private fun translateAndShow(text: String) {
        floatWindow.showResult("翻译中：$text")
        scope.launch {
            val result = runCatching { translator.translate(text) }.getOrDefault("翻译失败")
            floatWindow.showResult("原文：$text\n译文：$result")
        }
    }

    /** 递归收集一个视图节点及其所有子节点的文字（text + contentDescription） */
    private fun collectTexts(node: AccessibilityNodeInfo): List<String> {
        val result = mutableListOf<String>()
        node.text?.toString()?.let { if (it.isNotBlank()) result.add(it) }
        node.contentDescription?.toString()?.let { if (it.isNotBlank()) result.add(it) }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                result.addAll(collectTexts(child))
            }
        }
        return result
    }
}
