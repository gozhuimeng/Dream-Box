# Plan: GitHub 贡献热力图 Widget

## Problem

开发一个 Android 桌面小部件（Widget），展示 GitHub 用户的贡献热力图。
数据来源为 `ghchart.rshah.org/{color}/{username}`（返回 SVG）。
要求支持多用户、自定义颜色、刷新频率、安静时段、手动刷新等配置。
本项目定位为个人工具箱 App，架构需具备后续功能扩展能力。

## Environment

- OS: Arch Linux (rolling), Java 17 (Temurin), Git 2.54
- Gradle: 通过 Gradle Wrapper 自动管理
- Android SDK: 下载 cmdline-tools 到 `~/android-sdk/` 自行安装
- 无 sudo 权限 → 所有工具安装到用户目录

## Steps

### Phase 1: 项目初始化与工具链搭建

- [ ] Step 1: **搭建 Android SDK 环境** - 下载 cmdline-tools，安装 platform SDK 34、build-tools
  - Context: `~/android-sdk/` 作为 ANDROID_HOME
  - Files: 环境变量配置

- [ ] Step 2: **创建 Android 项目骨架** - 生成完整的 Gradle 项目结构
  - Context: 使用 Gradle Wrapper 8.x，包名 `com.zhuimeng.dreambox`
  - Files: `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties`, `gradle/wrapper/*`, `gradlew`

- [ ] Step 3: **初始化 Git 仓库 + 分支策略**
  - Context: `main` (保护) → `dev` (保护) → `feat/github-widget` (开发)
  - Files: `.gitignore`, 初始 commit 到 `main`，创建 `dev` 和 `feat/github-widget`

### Phase 2: Widget 核心功能实现

- [ ] Step 4: **创建 Widget 基础组件** - AppWidgetProvider + XML 布局 + WidgetInfo
  - Context: RemoteViews 仅支持有限 View 类型，SVG 需转为 Bitmap 后通过 ImageView 显示
  - Files: `GithubWidgetProvider.kt`, `github_widget_layout.xml`, `github_widget_info.xml`

- [ ] Step 5: **实现 SVG 获取与渲染** - 从 ghchart.rshah.org 下载 SVG 并渲染为 Bitmap
  - Context: 使用 `AndroidSVG`(com.caverock:androidsvg) 库渲染 SVG → Bitmap
  - Files: `GithubChartApi.kt` (网络请求), Bitmap 转换工具类

- [ ] Step 6: **实现配置存储系统** - 基于 DataStore 的 per-widget 配置
  - Context: 每个 widget 实例 (appWidgetId) 独立配置：用户名、颜色、刷新间隔、安静时段
  - Files: `WidgetConfig.kt` (数据模型), `WidgetConfigRepository.kt` (存储逻辑)

- [ ] Step 7: **实现 WorkManager 后台刷新** - 周期性 + 手动触发
  - Context: 安静时段判断逻辑（熄屏/夜间不请求）；手动刷新通过 PendingIntent
  - Files: `GithubWidgetWorker.kt`

### Phase 3: 配置界面与集成

- [ ] Step 8: **创建设置界面** - 管理所有 widget 实例的配置
  - Context: 列表展示所有已添加的 widget，每个可独立编辑用户名/颜色/刷新/安静时段
  - Files: `MainActivity.kt`, `activity_main.xml` / Compose UI, `WidgetListAdapter` 等

- [ ] Step 9: **注册 AndroidManifest** - 声明 Provider、权限、Activity
  - Context: INTERNET 权限，AppWidgetProvider 广播接收器，配置 Activity
  - Files: `AndroidManifest.xml`

### Phase 4: 集成测试与收尾

- [ ] Step 10: **构建验证与 AGENTS.md 更新**
  - Context: `./gradlew assembleDebug` 验证构建通过，更新 AGENTS.md 记录
  - Files: `AGENTS.md` 更新

## Dependencies

- Step 1 ← Step 2: 需要先有 SDK 才能 build
- Step 4/5/6 ← Step 2: 需要项目骨架
- Step 7 ← Step 5+6: Worker 依赖 SVG 获取和配置读取
- Step 8 ← Step 6: 设置界面依赖配置存储
- Step 9 ← Step 4+8: Manifest 注册所有组件
- Step 10 ← 所有: 最终验证

## Notes

- 多用户支持: 每个 widget 实例 (appWidgetId) 独立配置，可显示不同用户的贡献图
- SVG 渲染: RemoteViews 不支持直接显示 SVG，必须转为 Bitmap 后通过 `setImageViewBitmap` 设置
- 安静时段: 在 WorkManager 的 `doWork()` 中判断当前时间是否在安静时段内，是则跳过
- 后续扩展: 架构设计预留 `widget/` 目录，后续可添加其他 Widget；`ui/` 目录可扩展其他功能页面
- 环境限制: 无 sudo 权限，所有工具链均使用用户目录安装

## Git Branches

- `main` — 稳定版本
- `dev` — 开发集成分支
- `feat/github-widget` — 当前功能开发分支
- `fix/*` — Bug 修复分支

> 本期开发在 `feat/github-widget` 分支上进行，完成后合并到 `dev`，测试稳定后合入 `main`

## Current Status

- [x] Step 1: Android SDK 已安装 (platform 34, build-tools 34.0.0)
- [x] Step 2: 项目骨架创建完成，Gradle Wrapper 8.9
- [x] Step 3: Git 仓库初始化 (main → dev → feat/github-widget)
- [x] Step 4: Widget 基础组件 (Provider + RemoteViews 布局)
- [x] Step 5: SVG 获取 (GithubChartApi) + 渲染 (AndroidSVG→Bitmap)
- [x] Step 6: DataStore 配置存储 (per-appWidgetId 独立配置)
- [x] Step 7: WorkManager 后台刷新 (含安静时段判断)
- [x] Step 8: 设置界面 (Compose UI + ViewModel)
- [x] Step 9: AndroidManifest 组件注册
- [x] Step 10: ✅ 构建验证通过

## 下一步

- Bug 修复与测试
- 完成后合并到 dev 分支
