package com.example.screentranslate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.screentranslate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ① 去系统无障碍设置里开启本 App 的服务
        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // ② 去开启「悬浮窗（显示在其他应用上层）」权限
        binding.btnOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val a11yOn = isAccessibilityServiceEnabled(this, TranslateService::class.java)
        val overlayOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }

        binding.tvStatus.text = buildString {
            append("无障碍服务：").append(if (a11yOn) "已开启 ✅" else "未开启 ❌").append("\n")
            append("悬浮窗权限：").append(if (overlayOn) "已开启 ✅" else "未开启 ❌")
        }

        binding.btnAccessibility.isEnabled = !a11yOn
        binding.btnOverlay.isEnabled = !overlayOn
    }

    /** 判断本 App 的无障碍服务是否已在系统里启用 */
    private fun isAccessibilityServiceEnabled(
        context: Context,
        service: Class<*>
    ): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expected = "${context.packageName}/${service.name}"
        return enabled.split(":").any { it.equals(expected, ignoreCase = true) }
    }
}
