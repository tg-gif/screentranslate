# 保留无障碍服务，避免 release 打包时被混淆移除
-keep public class * extends android.accessibilityservice.AccessibilityService
-keep class com.example.screentranslate.** { *; }
