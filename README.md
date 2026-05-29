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
  <img src="https://img.shields.io/badge/minSdk-24-brightgreen" alt="minSdk 24">
  <img src="https://img.shields.io/badge/targetSdk-34-blue" alt="targetSdk 34">
  <img src="https://img.shields.io/badge/Kotlin-2.0-purple" alt="Kotlin 2.0">
  <img src="https://img.shields.io/badge/license-CC%20BY--NC--SA%204.0-orange" alt="License">
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
- ✅ **安静时段** — 设置夜间不刷新，省电省流量；支持跨天（如 22~7）
- ✅ **暗色主题** — 半透明暗色背景（`#E61C1B1F`）+ 12dp 圆角
- ✅ **亮色/暗色切换** — 每个 Widget 独立设置
- ✅ **手动刷新** — 一键立即更新，手动刷新跳过安静时段
- ✅ **Profile 配置管理** — 独立配置项（Profile），可被多个 Widget 复用，支持自定义名称
- ✅ **应用内管理** — Profile 列表，支持新建/编辑/删除
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
5. 在弹出的配置界面中选择已有的 **Profile**，或新建一个
6. 也可以**跳过**，稍后在 App 中管理

### 配置 / 管理

打开 **Dreambox** App，即可看到所有 Profile 列表：

- 点击 **编辑** — 修改用户名、颜色、刷新频率、安静时段、暗色主题等
- **点击 Widget** — 直接在桌面点击 Widget 主体，打开 Profile 选择器切换
- **Profile 复用** — 同一个 Profile 可被多个 Widget 同时使用
- **删除 Profile** — 已关联的 Widget 显示"配置已删除"提示

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

## 📂 项目结构 (v0.1.3)

```
app/
├── src/main/
│   ├── java/com/zhuimeng/dreambox/
│   │   ├── data/
│   │   │   ├── GithubChartApi.kt               # SVG 数据获取
│   │   │   ├── SvgRenderer.kt                  # SVG 裁剪/缩放/渲染
│   │   │   ├── WidgetProfile.kt                # Profile 数据模型
│   │   │   ├── WidgetProfileRepository.kt      # Profile DataStore 持久化
│   │   │   ├── WidgetMappingRepository.kt      # appWidgetId ↔ profileId 映射
│   │   │   └── MigrationHelper.kt              # 旧版配置 → Profile 迁移
│   │   ├── ui/
│   │   │   └── ProfileViewModel.kt             # Profile 管理 ViewModel
│   │   ├── widget/
│   │   │   ├── GithubWidgetProvider.kt          # 4x2 Widget Provider
│   │   │   ├── GithubWidgetTinyProvider.kt      # 2x1 Widget Provider
│   │   │   ├── GithubWidgetWorker.kt            # 后台刷新 Worker
│   │   │   └── WidgetConfigureActivity.kt       # Profile 选择器
│   │   ├── DreamboxApp.kt                      # Application 类（Hilt + WorkManager）
│   │   └── MainActivity.kt                     # 主界面 (Compose Profile 列表)
│   ├── res/
│   │   ├── drawable/
│   │   │   ├── widget_bg_light.xml             # 亮色圆角背景
│   │   │   └── widget_bg_dark.xml              # 暗色圆角背景
│   │   ├── layout/
│   │   │   ├── github_widget_layout.xml        # 4x2 布局
│   │   │   └── github_widget_layout_tiny.xml   # 2x1 布局
│   │   └── xml/
│   │       ├── github_widget_info.xml          # 4x2 元数据
│   │       └── github_widget_tiny_info.xml     # 2x1 元数据
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

打开 Dreambox App → 编辑对应 Profile → 修改颜色（16 进制，如 `#39D353`）。

### 为什么无法从 App 内添加 Widget？

Android 桌面 Widget 必须从**桌面长按 → 添加 Widget** 操作。App 内的"添加 Widget"按钮仅提供引导说明。

### 什么是 Profile？

v0.1.3 引入了 **Profile（配置项）** 概念：一个 Profile 包含一组完整的 Widget 配置（用户名、颜色、刷新频率、安静时段、暗色主题），可以独立创建/编辑/删除，也可以被多个 Widget 同时引用。这样你只需要配置一次，就能让桌面上的多个 Widget 展示相同的内容。

---

## 🤖 开发说明

本项目**全部代码**均由 **AI 大语言模型（LLM）与 AI Agent** 自动生成，包括但不限于：
- 项目架构设计与实现
- 所有 Kotlin/XML/Gradle 代码
- 文档（README、AGENTS.md 等）
- Git 提交与发布流程

人类开发者的角色仅为：
- 提出功能需求和验收标准
- 通过自然语言指令引导 AI 生成代码
- 在 Android 真机上进行功能测试
- 提供网络代理等运行环境支持

> ⚠️ **免责声明**：本项目为 AI 生成实验性作品，作者及 LLM 平台（包括但不限于 DeepSeek 等）**不对因使用本项目造成的任何直接或间接损失负责**，包括但不限于数据丢失、设备故障、隐私泄露或其他意外后果。使用本软件即视为您已理解并接受此风险。

---

## 📄 许可证

---

<p align="center">
  <sub>用 ❤️、Kotlin 和 AI 打造 | 个人项目 · 持续进化中</sub>
</p>
