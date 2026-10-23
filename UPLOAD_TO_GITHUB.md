# 上传到 GitHub，云端编译出 APK（零安装，全程约 10 分钟）

目标：不装 Android Studio / JDK / SDK，让 GitHub 云服务器帮你编译出 `app-debug.apk`，下载后传到手机安装。

> 需要你有一个 GitHub 账号（没有就用邮箱免费注册一个）。
> 上传会用到你的账号，所以这几步需要你本人在网页点，我无法代劳——这是对你账号最安全的做法。

---

## 第 1 步：建一个空仓库
1. 登录 github.com → 右上角 **+** → **New repository**。
2. **Repository name** 填 `screentranslate`（随意）。
3. 选 **Public** 或 **Private** 都行（Private 也能用 Actions，每月免费额度足够）。
4. **不要**勾选 "Add a README"（保持空仓库，避免冲突）。
5. 点 **Create repository**。

## 第 2 步：把项目文件传上去
1. 在建好的仓库页，点 **Add file → Upload files**。
2. 打开本机文件夹 `ScreenTranslate`，把**里面的所有内容**拖进网页上传区。
   - ⚠️ **务必包含隐藏的 `.github` 文件夹**（编译流水线就在里面）和 `.gitignore`。
   - Windows 资源管理器默认可能不显示点开头的文件夹：在资源管理器顶部「查看」勾选「隐藏的项目」，确认 `.github` 能看到，再拖。
   - 稳妥做法：直接选中文件夹内全部条目（含 `.github`）一起拖。
3. 等上传进度走完，点 **Commit changes**。

> 如果上传后 Actions 没反应，多半是 `.github` 没传上去。用第 4 步的「兜底」手动补一个文件即可。

## 第 3 步：等云端自动编译
1. 仓库页顶部点 **Actions** 标签。
2. 会看到一条 **Build APK** 工作流正在跑（黄点转圈）。
3. 首次约 2–5 分钟，变成绿色 ✅ 即成功。
   - 若失败（红叉），点进去看日志，把报错发我（见文末常见问题）。

## 第 4 步：下载 APK
1. 点进那条成功的 **Build APK** 记录，拉到最下方 **Artifacts** 区。
2. 点 **app-debug-apk**（一个 zip），下载后解压，得到 `app-debug.apk`。

## 第 5 步：装到手机
1. 把 `app-debug.apk` 传到手机：微信/QQ 文件传输、数据线拷贝、或邮件均可。
2. 手机上点击该 apk → 提示「未知来源」时允许安装。
3. 装好打开，按 App 内 **①开启无障碍服务**、**②开启悬浮窗权限**。
4. 回桌面看到紫色翻译球即成功。打开任意日文界面 → 点球 → 「翻译整个界面」。

---

## 兜底：如果 Actions 没触发（.github 没上传成功）
在仓库页 **Add file → Create new file**，文件名填：
```
.github/workflows/android.yml
```
然后把下面内容原样粘贴进去，点 **Commit changes**，Actions 会自动开始编译：

```yaml
name: Build APK
on:
  push:
    branches: [ main, master ]
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - name: Setup Android SDK
        uses: android-actions/setup-android@v3
        with:
          packages: |
            platforms;android-34
            build-tools;34.0.0
      - name: Setup Gradle 8.7
        uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.7'
      - name: Build Debug APK
        run: gradle assembleDebug --no-daemon --stacktrace
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: app-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
          if-no-files-found: error
```

---

## 可选：你电脑有 git 的话，命令行推送更快
先按第 1 步建好空仓库，然后在 `ScreenTranslate` 文件夹执行：
```bash
git init
git add .
git commit -m "init: screen translate app"
git branch -M main
git remote add origin https://github.com/<你的用户名>/screentranslate.git
git push -u origin main
```
（会弹浏览器或终端让你登录 GitHub，按提示授权即可。）

---

## 常见问题
- **Actions 页面空、没有 Build APK**：`.github` 没上传成功，用上面「兜底」手动新建 workflow。
- **构建报 Gradle/SDK 下载超时**：在该次运行右上角选 **Re-run jobs** 重跑，通常是网络波动。
- **报 `Minimum supported Gradle version`**：确认 workflow 里 gradle-version 是 `8.7`（已内置修正）。
- **Artifacts 里没有 apk**：构建失败了，点进红叉看日志，把最后几行发我。
- **apk 装了打不开/闪退**：把手机上的报错现象告诉我，我来定位。
