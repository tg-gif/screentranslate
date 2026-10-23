# 界面翻译（ScreenTranslate）

一个安卓 App：**悬浮窗常驻，把手机上任意 App 界面显示的文字翻译成中文。**

通过系统「无障碍服务」读取屏幕上其他 App 的视图文字（比截图 OCR 更准、更快），
再用免费翻译 API 翻译成中文，结果在悬浮面板里以「原文 → 译文」对照展示。

---

## 它能做什么

- 桌面常驻一个紫色翻译球，可拖动。
- **翻译整个界面**：点开球 → 「翻译整个界面」，会把当前屏幕所有可读文字抓取下来逐段翻译。
- **取词翻译**：开启「取词模式」后，点击屏幕上「可点击 / 可长按」的文字，单独翻译那一段。
- 无需注册、无需 API Key，开箱即用（默认用 MyMemory 免费接口）。

## 它做不到什么（先说清楚，免得踩坑）

1. **不是「全自动实时覆盖原文字」**。当前版本是「按需翻译」：你手动触发后，
   译文出现在悬浮面板里对照看，不会直接把原 App 上的字改成中文。
   要做到「原地替换文字」需要自己画一层覆盖在界面上重绘，工程量大，是进阶方向。
2. **取词对纯文本有效，但对「不可点击」的静态文字点不动**。
   这类文字用「翻译整个界面」更稳。
3. **翻译接口额度有限**。MyMemory 匿名约 5000 词/天，日常够用；量大请换 LibreTranslate / 有道 / 百度（见下文）。
4. **源语言默认面向日文**：已内置自动探测——含日文假名按 ja 翻、纯汉字判为中文直接跳过、其余按 en 兜底，英日混杂也能各自翻对。

---

## 工作原理（给不熟悉安卓的你）

```
无障碍服务(AccessibilityService)
   ├─ 能读取当前屏幕所有 App 的视图树（每个按钮/文本框/标签都是节点）
   ├─ 收集节点上的文字 → 调用翻译接口 → 中文
   └─ 通过悬浮窗(Overlay) 把结果画在屏幕最上层
```

为什么必须「无障碍服务 + 悬浮窗」这两个权限？
- 普通 App 看不到别的 App 屏幕上写了啥，这是安卓的隐私隔离。
- 无障碍服务是系统合法提供的「帮残障用户读屏」能力，我们借它来拿文字。
- 悬浮窗权限让 App 能把内容画在所有界面最上层。

---

## 如何编译并装到手机

> 想不装任何开发环境、直接拿到能装的 APK：看 `UPLOAD_TO_GITHUB.md`（GitHub 云端编译，约 10 分钟，推荐）。
> 想在自己电脑上编译：按下面的 Android Studio 步骤。

### 准备
1. 电脑装 **Android Studio**（官网免费）。
2. 手机：设置 → 关于手机 → 连点「版本号」7 次，打开**开发者选项** → 打开 **USB 调试**。
3. 用数据线连电脑，手机弹窗选「允许 USB 调试」。

### 打开项目
- Android Studio → Open → 选择本文件夹 `ScreenTranslate`。
- 首次打开会下载 Gradle 和 SDK，等它同步完成（底部进度条走完）。
- 若提示缺少 Gradle wrapper，让 AS 自动创建即可。

### 运行
- 菜单 Run → Run 'app'，选你的手机设备。
- 编译出 APK 并自动安装。首次可能要在手机上点「继续安装」。

> 不想要 Android Studio 图形界面，也可以命令行：
> 在项目根目录执行 `./gradlew assembleDebug`，
> 生成的 APK 在 `app/build/outputs/apk/debug/`，拷到手机安装即可
> （命令行需要你自己先装好 JDK 17 和 Android SDK）。

### 开启权限（装好后）
1. 打开 App，按 **① 开启无障碍服务** → 在系统列表里找到「界面翻译服务」→ 开启。
2. 返回，按 **② 开启悬浮窗权限** → 允许「显示在其他应用上层」。
3. 回到桌面，看到紫色翻译球就成功了。

---

## 怎么换成更稳的翻译引擎

默认实现在 `app/src/main/java/com/example/screentranslate/translator/MyMemoryTranslator.kt`。

想换引擎：新建一个类实现 `Translator` 接口，例如：

```kotlin
class LibreTranslate : Translator {
    override suspend fun translate(text: String): String {
        // 调用你的 LibreTranslate 实例或申请了 Key 的云服务
        // 返回中文译文字符串即可
    }
}
```

然后在 `TranslateService.kt` 把这一行换掉：
```kotlin
private val translator: Translator = MyMemoryTranslator()
// 改成
private val translator: Translator = LibreTranslate()
```

常用可选项：
- **LibreTranslate**：开源，可自建或找公共实例，`source: "auto"` 能自动识别源语言。
- **有道智云 / 百度翻译**：国内访问快、稳，但需要注册拿到 `appKey` + `secret`，按文档拼签名。

---

## 目录结构

```
ScreenTranslate/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/screentranslate/
│       │   ├── MainActivity.kt        # 引导开启两个权限的主界面
│       │   ├── TranslateService.kt    # 无障碍服务：读屏 + 整页/取词翻译
│       │   ├── FloatWindow.kt         # 悬浮球 + 结果面板
│       │   └── translator/
│       │       ├── Translator.kt          # 翻译接口
│       │       └── MyMemoryTranslator.kt  # 默认免费实现
│       ├── res/
│       │   ├── layout/   # activity_main.xml / float_window.xml
│       │   ├── drawable/ # 图标与背景
│       │   ├── values/   # 字符串/颜色/主题
│       │   └── xml/      # accessibility_service_config.xml
├── build.gradle
├── settings.gradle
└── README.md
```

---

## 已知问题 / 后续可加

- 整页翻译是「列表对照」，不是原地替换；进阶可做覆盖层重绘。
- 取词依赖视图可点击，纯静态文字请用整页翻译。
- 已内置粗略的源语言探测（日文优先），但非专业级语言识别；混合长句可能误判，需要更准可换 LibreTranslate 的 auto 检测。
- 可加：翻译结果缓存、常用语种切换、翻译球长按快捷操作。
