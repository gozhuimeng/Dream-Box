<p align="right">
  <a href="README_EN.md">🌐 English</a>
</p>

<h1 align="center">Dreambox · 坠梦</h1>

<p align="center">
  <em>个人 Android 工具箱 — 始于 GitHub 贡献热力图 Widget</em>
</p>

<p align="center">
  <a href="https://github.com/gozhuimeng/Dream-Box/releases">
    <img src="https://img.shields.io/github/v/release/gozhuimeng/Dream-Box?label=版本" alt="Release">
  </a>
  <img src="https://img.shields.io/badge/license-CC%20BY--NC--SA%204.0-orange" alt="License">
  <img src="https://img.shields.io/badge/minSdk-24-brightgreen" alt="minSdk 24">
  <img src="https://img.shields.io/badge/targetSdk-34-blue" alt="targetSdk 34">
  <img src="https://img.shields.io/badge/Kotlin-2.0-purple" alt="Kotlin 2.0">
</p>

## 📄 许可协议

本作品采用 **署名-非商业性使用-相同方式共享 4.0 国际 (CC BY-NC-SA 4.0)** 许可。

- ✅ **允许** — 个人学习、修改、非商业性分享
- ⚠️ **必须** — 署名原作者，以相同方式共享衍生作品
- ❌ **禁止** — 任何商业用途

完整协议文本见 [LICENSE](LICENSE) 文件。

---
**Dreambox（坠梦）** 是一款个人 Android 工具箱应用，当前唯一功能是 **GitHub 贡献热力图桌面 Widget**。

将你的 GitHub 贡献热力图直接放在手机桌面上，无需打开 App 即可一目了然 —— 支持 4x2 和 2x1 两种尺寸。

> 数据来源为 [ghchart.rshah.org](https://ghchart.rshah.org)，**无需 API Key，无需 GitHub OAuth 授权**。

---

## ✨ 功能特性

- ✅ **GitHub 贡献热力图 Widget** — 4x2 标准尺寸（用户名 + 热力图 + 时间戳 + 刷新按钮）
- ✅ **2x1 迷你 Widget** — 仅热力图，极简布局
- ✅ **多用户支持** — 每个 Widget 实例独立配置 GitHub 用户名
- ✅ **自定义颜色** — 支持任意 16 进制色码
- ✅ **自定义刷新频率** — 可配置刷新间隔
- ✅ **安静时段** — 设置夜间不刷新，省电省流量
- ✅ **暗色主题** — 半透明暗色背景（`#E61C1B1F`）+ 12dp 圆角
- ✅ **亮色/暗色切换** — 每个 Widget 独立设置
- ✅ **手动刷新** — 一键立即更新
- ✅ **应用内管理** — 列表查看、编辑、删除所有已配置的 Widget
- ✅ **MIUI 兼容** — 修复了 MIUI 桌面频繁调用导致的闪烁问题

---

## 📱 截图

> *（截图待补充）*

| Widget 类型 | 亮色主题 | 暗色主题 |
|:---:|:---:|:---:|
| **4x2 标准** | — | — |
| **2x1 迷你** | — | — |

---

## 🚀 快速开始

### 下载安装

从 [GitHub Releases](https://github.com/gozhuimeng/Dream-Box/releases) 下载最新 APK 并安装。

### 添加 Widget

1. 在桌面**长按空白处**
2. 选择 **Widgets / 小工具**
3. 找到 **Dreambox**
4. 选择 **4x2** 或 **2x1** 尺寸添加到桌面
5. 在弹出的配置界面中输入 GitHub 用户名
6. 或**跳过配置**，稍后在 App 中编辑

### 配置 / 管理

打开 **Dreambox** App，即可看到所有已添加的 Widget 列表：

- 点击 ✏️ **编辑** — 修改用户名、颜色、刷新频率、暗色主题等
- 点击 🔄 **刷新** — 立即更新 Widget
- 点击 🗑️ **删除** — 删除配置（Widget 也会从桌面移除）

---

## 🔧 从源码构建

```bash
# 克隆仓库
git clone git@github.com:gozhuimeng/Dream-Box.git
cd Dream-Box

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK（已使用 debug keystore 签名）
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug
```

> **注意**：Release APK 使用 debug keystore 签名，可以正常安装。如需发布到应用商店，请替换为正式签名。

---

## 🏗️ 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin 2.0 |
| UI | Jetpack Compose + Material Design 3 |
| 架构 | MVVM + Clean Architecture |
| 依赖注入 | Hilt |
| 后台任务 | WorkManager |
| 数据存储 | DataStore Preferences |
| 网络请求 | OkHttp |
| SVG 渲染 | AndroidSVG |
| Widget | RemoteViews + AppWidgetProvider |
| 最低 SDK | Android 7.0 (API 24) |
| 目标 SDK | Android 14 (API 34) |

---

## 📂 项目结构

```
app/
├── src/main/
│   ├── java/com/zhuimeng/dreambox/
│   │   ├── data/
│   │   │   ├── GithubChartApi.kt       # SVG 数据获取
│   │   │   ├── SvgRenderer.kt           # SVG 裁剪/缩放/渲染
│   │   │   ├── WidgetConfig.kt          # 配置数据模型
│   │   │   └── WidgetConfigRepository.kt # DataStore 持久化
│   │   ├── ui/
│   │   │   └── WidgetSettingsViewModel.kt # 设置界面 ViewModel
│   │   ├── widget/
│   │   │   ├── GithubWidgetProvider.kt    # 4x2 Widget Provider
│   │   │   ├── GithubWidgetTinyProvider.kt # 2x1 Widget Provider
│   │   │   ├── GithubWidgetWorker.kt      # 后台刷新 Worker
│   │   │   └── WidgetConfigureActivity.kt # 配置 Activity
│   │   ├── DreamboxApp.kt               # Application 类
│   │   └── MainActivity.kt              # 主界面 (Compose)
│   ├── res/
│   │   ├── drawable/
│   │   │   ├── widget_bg_light.xml      # 亮色圆角背景
│   │   │   └── widget_bg_dark.xml       # 暗色圆角背景
│   │   ├── layout/
│   │   │   ├── github_widget_layout.xml      # 4x2 布局
│   │   │   └── github_widget_layout_tiny.xml # 2x1 布局
│   │   └── xml/
│   │       ├── github_widget_info.xml        # 4x2 元数据
│   │       └── github_widget_tiny_info.xml   # 2x1 元数据
```

---

## ❓ 常见问题

### Widget 显示"加载中..."或空白？

- 检查网络连接是否正常
- 检查 GitHub 用户名是否正确（注意大小写）
- Wi-Fi / 移动数据下均需要联网权限
- 首次添加后可能需要等待 1-2 次刷新周期

### MIUI / 小米手机 Widget 闪烁？

该问题已在 v0.1.1 中修复。如果仍然遇到，尝试在桌面重新添加 Widget。

### 如何修改 Widget 颜色？

打开 Dreambox App → 点击 Widget 旁的 ✏️ → 修改颜色（16 进制，如 `#39D353`）。

### 为什么无法从 App 内添加 Widget？

Android 桌面 Widget 必须从**桌面长按 → 添加 Widget** 操作。App 内的"添加 Widget"按钮仅提供引导说明。

---

## 📄 许可证

---



<p align="center">
  <sub>用 ❤️ 和 Kotlin 打造 | 个人项目 · 持续进化中</sub>
</p>
