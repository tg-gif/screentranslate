package com.example.screentranslate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.example.screentranslate.databinding.FloatWindowBinding

/**
 * 悬浮窗：一个可拖动的紫色翻译球 + 一个结果面板。
 *
 * 注意：悬浮窗依赖「显示在其他应用上层」权限（SYSTEM_ALERT_WINDOW），
 * 没有这个权限 addView 会直接抛异常，所以 show 前先检查。
 */
class FloatWindow(
    private val context: Context,
    private val callback: Callback
) {

    interface Callback {
        fun onTogglePickMode()
        fun onTranslateScreen()
    }

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val binding: FloatWindowBinding =
        FloatWindowBinding.inflate(LayoutInflater.from(context))

    private val params: WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = 240
    }

    private var lastX = 0f
    private var lastY = 0f
    private var downX = 0f
    private var downY = 0f
    private var moved = false

    init {
        binding.ball.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    lastX = params.x.toFloat()
                    lastY = params.y.toFloat()
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (kotlin.math.abs(dx) > 5 || kotlin.math.abs(dy) > 5) moved = true
                    params.x = (lastX + dx).toInt()
                    params.y = (lastY + dy).toInt()
                    windowManager.updateViewLayout(binding.root, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // 没怎么移动算「点击」，展开/收起面板
                    if (!moved) togglePanel()
                    true
                }
                else -> false
            }
        }

        binding.btnPick.setOnClickListener { callback.onTogglePickMode() }
        binding.btnScreen.setOnClickListener { callback.onTranslateScreen() }
        binding.btnClose.setOnClickListener {
            binding.panel.visibility = View.GONE
        }
    }

    private fun togglePanel() {
        binding.panel.visibility =
            if (binding.panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    fun show() {
        if (binding.root.parent == null) {
            windowManager.addView(binding.root, params)
        }
    }

    fun hide() {
        if (binding.root.parent != null) {
            windowManager.removeView(binding.root)
        }
    }

    fun setPickMode(on: Boolean) {
        binding.btnPick.text = if (on) "取词：开(点屏幕文字)" else "取词模式"
    }

    fun showResult(text: String) {
        binding.panel.visibility = View.VISIBLE
        binding.result.text = text
    }

    /** 检查并引导开启悬浮窗权限，返回是否已有该权限 */
    fun ensureOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(context)
        ) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return false
        }
        return true
    }
}
