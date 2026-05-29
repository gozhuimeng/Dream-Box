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
- `feat/github-widget` — 已完成的功能开发分支
- `fix/*` — Bug 修复分支

> 本期开发已在 `feat/github-widget` 分支上完成，已合并到 `dev`。

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
- [x] Step 10: 构建验证通过

## 修复记录

- [x] v0.1.1: Widget 闪烁问题修复
  - `onUpdate` 不再修改 Widget UI（只触发后台 Worker）
  - 添加 30 秒防抖过滤 MIUI 频繁调用
  - Worker 使用唯一 Work + KEEP 策略防止并发
  - 未配置用户名的 Widget 跳过不更新 UI

- [x] v0.1.2: 新增功能 + 质量修复
  - 新增 2x1 迷你 Widget（GithubWidgetTinyProvider）
  - 新增暗色主题（半透明背景 + 12dp 圆角）
  - 新增亮色/暗色圆角背景 drawable
  - SVG 视觉缩放：裁剪左侧标签 + 放大填满 + 右侧对齐
  - 跳过配置后保存默认配置到 DataStore
  - Widget 删除时清理 DataStore（onDeleted）
  - 编辑对话框添加暗色主题开关
  - FAB 改为引导提示（从桌面添加）
  - 编辑保存时正确传递 theme 字段

## 下一步

- [x] 功能开发完成，已合并到 `dev`
- [x] v0.1.2 Release 发布
- [ ] v0.1.3: 架构重构 — 配置项与 Widget 解耦

---

## v0.1.3 计划: 配置项与 Widget 解耦

### 痛点分析

当前问题:
1. 必须先添加 Widget，才能配置（"创建小部件，然后填好配置"）
2. 配置绑定 appWidgetId，Widget 删除后配置也消失
3. 没有"独立配置项"的概念，无法预先创建多套配置

### 目标

```
旧: Widget ← 1:1 → 配置（毁 widget = 丢配置）
新: Widget → 引用 → Profile（独立配置项，可复用）
                        ↑
                    App 内自由创建/编辑/删除
```

### 详细方案

#### 1. 数据模型重构

| 当前 (v0.1.2) | 新 (v0.1.3) |
|---|---|
| `WidgetConfig(appWidgetId, username, color, ...)` | `WidgetProfile(id, name, username, color, ...)` |
| key = appWidgetId | key = profileId (UUID) |
| 无用户命名 | 新增 `name` 字段（如"工作号"、"小号"） |

新增映射表: `appWidgetId → profileId`（一个 profile 可被多个 widget 引用）

#### 2. 存储层变化

- **Profile 存储**: 独立 DataStore（或同一 DataStore 用不同 key prefix）
  - `profile_${profileId}_name`
  - `profile_${profileId}_username`
  - `profile_${profileId}_color`
  - etc.
- **映射存储**: `widget_profile_map` DataStore
  - `widget_${appWidgetId}_profile` → profileId

#### 3. 界面变化

- **App 主页** → 显示 **Profile 列表**（不是 widget 列表）
  - 每个卡片: profile name, username, 预览颜色, 已关联 widget 数
  - FAB: 新建 Profile
  - 点击: 编辑 Profile
  - 长按/侧滑: 删除 Profile
- **移除** 旧有的"以 widget 为中心"的列表

#### 4. Widget 配置流程变化

- **添加 Widget** → `WidgetConfigureActivity` 打开
  - 显示所有已有 Profile 列表供选择
  - 底部"新建 Profile"按钮
  - 选择后 → widget 绑定该 profile
- **点击 Widget** → 打开 App 到 Profile 列表（或快速切换 Profile）

#### 5. 刷新逻辑变化

- `GithubWidgetWorker` 以 **profile** 为单位刷新
- 刷新前检查 profile 是否被至少一个活跃 widget 引用
- 未被引用的 profile 跳过刷新（省流量省电）
- `onDeleted`: 检查是否还有其他 widget 引用该 profile，无则停止刷新

#### 6. 配置项管理（新增功能）

- **独立创建**：在 App 中直接新建 Profile，填写用户名/颜色/主题等
- **独立删除**：删除 Profile 时，已关联的 widget 显示"配置已删除"提示
- **复用**：同一个 Profile 可被多个 Widget 引用
- **改名**：Profile 支持自定义名称

### 主要影响文件

| 文件 | 变化 |
|------|------|
| `data/WidgetConfig.kt` | 重命名为 `WidgetProfile.kt`，添加 `id` 和 `name` 字段 |
| `data/WidgetConfigRepository.kt` | 重写为 Profile 存储 + widget→profile 映射 |
| `widget/WidgetConfigureActivity.kt` | 改为 Profile 选择界面 |
| `widget/GithubWidgetProvider.kt` | onUpdate 改为读取 profile |
| `widget/GithubWidgetWorker.kt` | 按 profile 刷新，检查引用 |
| `ui/WidgetSettingsViewModel.kt` | 重构为 Profile 管理 |
| `MainActivity.kt` | 改为 Profile 列表 UI |
| `AGENTS.md` | 更新记录 |

### 注意事项

- **向下兼容**: 旧版用户已有 widget 配置需要迁移为 Profile
- **UUID 生成**: Profile ID 使用 `UUID.randomUUID().toString()`
- **不展示不刷新**: 核心原则 — 没有被任何 widget 引用的 profile 不触发网络请求
