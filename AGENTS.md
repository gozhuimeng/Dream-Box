# AGENTS.md - Android Project

## Active Plans
- [PLAN.md](./PLAN.md) - GitHub 贡献热力图 Widget 开发计划 (v0.1.3)

## 项目信息
- 项目名: **dreambox** (坠梦)
- 包名: `com.zhuimeng.dreambox`
- 定位: 个人工具箱 App
- 最低SDK: 24 / 目标SDK: 34
- 当前状态: 配置项解耦 + 安静时段优化 + Worker 健壮性增强
- 开发分支: `dev`

## 网络代理策略
- 请求默认设置 30 秒超时，**不自动使用代理**
- 失败后重试时使用代理: `http://127.0.0.1:2080`
- 在 bash 命令中通过 `curl -x` 或 `export http_proxy` 选择性使用

## 构建命令
```bash
./gradlew assembleDebug         # 构建Debug APK
./gradlew assembleRelease       # 构建Release APK
./gradlew installDebug          # 安装到设备
./gradlew test                  # 运行单元测试
./gradlew test --tests "com.zhuimeng.dreambox.ClassName.method"  # 单个测试
./gradlew connectedAndroidTest # Instrumented测试
./gradlew lint                  # Lint分析
./gradlew ktlintFormat          # 自动格式化
./gradlew clean && ./gradlew dependencies  # 清理+查看依赖
```

## 代码规范

### 命名
- 类/接口: `PascalCase` | 函数/变量: `camelCase` | 常量: `SCREAMING_SNAKE_CASE`
- 包名全小写 | 枚举值用 `camelCase` 或 `SCREAMING_SNAKE_CASE`
- 资源命名: 小写+下划线 (布局`activity_main.xml`, ID`btn_submit`, 颜色`color_primary`)

### Kotlin
- 优先用 `lateinit` / `by lazy`，避免可空类型滥用
- 使用 `?.` 和 `?:` 操作符
- 函数不超过40行，参数不超过5个
- 使用命名参数提高可读性

### 导入顺序
```kotlin
import android.* → androidx.* → com.google.* → com.zhuimeng.* → kotlin.* → java.*
```

### 注释
- 公共API用 KDoc 风格
- 无用代码直接删除，不注释

### 错误处理
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}
```

### 依赖注入 (Hilt)
```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity()

@HiltViewModel
class MyViewModel @Inject constructor(private val repo: MyRepository) : ViewModel()
```

## 架构 (MVVM + Clean Architecture)
```
presentation/  → ui/(Activity, Fragment, Composables), viewmodel/, adapter/
domain/        → model/, usecase/, repository/ (接口)
data/          → repository/ (实现), remote/ (API), local/ (存储)
```

## Android 注意事项
- Context: Activity用 `this`，ViewHolder用 `itemView.context`
- 生命周期: `onDestroy` 清理资源，用 `LifecycleScope` 管协程
- 建议用 `Flow` 替代 `LiveData`

## Widget 开发
- 用 `RemoteViews`，不支持复杂UI
- 用 `AppWidgetProvider` 管理生命周期
- 数据更新用 `WorkManager`，刷新≤15分钟
- 需要权限: `<uses-permission android:name="android.permission.INTERNET" />`
- SVG 需通过 `AndroidSVG` 库转为 Bitmap 后显示

## 当前功能: GitHub 贡献热力图 Widget

### 数据源
- `https://ghchart.rshah.org/{十六进制颜色}/{用户名}` → SVG
- 无需 API Key，无需 OAuth

### 功能需求
- [x] 多 GitHub 用户支持 (每个 widget 实例独立配置)
- [x] 自定义颜色 (16进制色码)
- [x] 自定义刷新频率
- [x] 手动立刻刷新
- [x] 安静时段 (不刷新时间段, 如夜间)
- [x] 应用内配置界面

### 配置存储
- DataStore Preferences
- 按 appWidgetId 存储: username, color, refreshInterval, quietHours

### 关键修复: Widget 闪烁问题 (v0.1.1)
- **问题**: MIUI 桌面每 ~1 秒调用 `onUpdate`，导致 Widget 在「加载中...」和热力图之间闪烁
- **根因**: `onUpdate` → `updateWidgetUi("加载中...")` → `updateAppWidget()` → MIUI 再次触发 `onUpdate` 的恶性循环
- **修复方案**:
  - `onUpdate` 不再修改 Widget UI（只触发后台 Worker），打破循环
  - 添加 30 秒防抖 (`lastEnqueueTime`)，过滤 MIUI 频繁调用
  - `enqueueRefresh` 使用 `enqueueUniqueWork` + `ExistingWorkPolicy.KEEP`，防止同一 Widget 多个 Worker 并发
  - Worker 对未配置用户名的 Widget 跳过不更新 UI
- **影响文件**: `GithubWidgetProvider.kt`, `GithubWidgetWorker.kt`, `WidgetSettingsViewModel.kt`

## 开发流程记录

### Phase 1: 项目搭建 (已完成 ✅)
| 步骤 | 状态 | 问题 | 解决 |
|------|------|------|------|
| Android SDK 环境 | ✅ | 无 sudo 权限，需手动安装到 ~/android-sdk/ | 使用 cmdline-tools 安装到用户目录 |
| Gradle Wrapper | ✅ | 需代理下载 Gradle 8.9 分发包 | `http_proxy` + `https_proxy` 同时设置 |
| Git 仓库初始化 | ✅ | — | main → dev → feat/github-widget |

### Phase 2: Widget 核心功能 (已完成 ✅)
| 步骤 | 状态 | 问题 | 解决 |
|------|------|------|------|
| Widget Provider + 布局 | ✅ | — | RemoteViews + AppWidgetProvider |
| SVG 获取与渲染 | ✅ | — | OkHttp + AndroidSVG 库 |
| DataStore 配置存储 | ✅ | — | 按 appWidgetId 独立存储 |
| WorkManager 后台刷新 | ✅ | — | 周期性 + 手动触发，含安静时段 |
| 配置界面 | ✅ | — | Compose UI + ViewModel |

### Phase 3: 集成与修复 (已完成 ✅)
| 步骤 | 状态 | 问题 | 解决 |
|------|------|------|------|
| Hilt + WorkManager 集成 | ⚠️ 已修复 | `NoSuchMethodException: GithubWidgetWorker.<init>` | 禁用 WorkManager auto-init，在 `DreamboxApp.onCreate()` 中手动初始化 |
| 配置 Activity | ✅ | WidgetConfigureActivity 不可用 | 补齐完整的配置表单 |
| Widget 闪烁 | ⚠️ 已修复 | MIUI 桌面每 ~1秒 调用 `onUpdate`，导致「加载中...」闪烁 | 三步修复：onUpdate 不改 UI、30 秒防抖、UniqueWork KEEP 策略 |
| SVG 视觉缩放 | ✅ | 热力图仅占 4x2 中间 4x1 | 裁剪左侧标签 + 放大填满 + 显示右侧最新贡献 |
| 刷新时的 MIUI 小窗提示 | ✅ 已解决 | MIUI 弹出"加载失败"系统小窗 | 修复闪烁后不再出现 |

### Phase 4: 新增功能 (已完成 ✅)
| 需求 | 状态 | 说明 |
|------|------|------|
| 2x1 迷你 Widget | ✅ | 仅显示热力图，无文字信息，独立 Provider 继承自主类 |
| 暗色背景（半透明） | ✅ | 配置界面可选，背景 `#E61C1B1F` 带 12dp 圆角 |
| 圆角 | ✅ | 亮色/暗色背景均带 12dp 圆角 |
| 多尺寸共存 | ✅ | 4x2 和 2x1 各自独立创建实例 |

### Phase 5: 质量修复 (已完成 ✅)
| 问题 | 修复方案 |
|------|---------|
| 跳过配置后 Widget 不可见 | 跳过时保存默认配置到 DataStore，Widget 自动出现在设置列表 |
| Widget 删除后 DataStore 残留 | `onDeleted` 中调用 `deleteWidgetConfig` 清理 |
| 编辑对话框缺少主题开关 | 在 `WidgetConfigDialog` 中添加暗色主题 Switch |
| FAB "添加 Widget" 产生无效配置 | 改为 Toast 提示用户从桌面添加 |
| 编辑对话框保存时未传 theme | `config.copy()` 中包含 `theme` 字段 |

### Phase 6: 架构重构 — 配置项与 Widget 解耦 (v0.1.3)
| 需求 | 状态 | 说明 |
|------|------|------|
| Profile 数据模型 | ✅ | 独立配置项 `WidgetProfile`，使用 UUID 作为 ID |
| Profile 管理界面 | ✅ | App 主页改为 Profile 列表，支持新建/编辑/删除 |
| Widget 引用 Profile | ✅ | `WidgetMappingRepository` 管理 appWidgetId ↔ profileId 映射 |
| Widget 新增流程 | ✅ | 添加 Widget → 选择已有 Profile 或新建 |
| 点击 Widget → 配置页 | ✅ | 点击 Widget 主体打开 Profile 选择器，可切换/新建 |
| 按 Profile 刷新 | ✅ | Worker 通过映射读取 Profile，未被引用的不刷新 |
| 配置复用 | ✅ | 同一 Profile 可被多个 Widget 同时使用 |
| 向下兼容 | ✅ | 首次启动自动迁移旧版 WidgetConfig → Profile |

### Phase 7: v0.1.3 Bugfix — Worker 健壮性 + 安静时段修复
| 问题 | 状态 | 修复方案 |
|------|------|---------|
| 切换 Profile 后 Widget 不刷新（卡在"更新中..."） | ✅ | 根因: 切换到的 Profile 设了安静时段(22~7) → Worker 在夜间卡住 |
| 安静时段导致 Widget 卡住 | ✅ | 将安静时段检查移到 "更新中..." 设置之前，避免卡死 |
| 手动刷新被安静时段拦截 | ✅ | `forceRefresh()` 传入 `KEY_FORCE_REFRESH=true` 跳过安静时段检查 |
| forceRefresh 竞态条件 | ✅ | 移除 cancel + enqueue 模式，统一用 `ExistingWorkPolicy.REPLACE` |
| SVG 获取无超时保护 | ✅ | 添加 `withTimeout(60s)` + `withContext(IO)` |
| Worker catch 块无回退 | ✅ | catch 块中 DataStore 读取失败时，回退显示占位错误 UI |
| Widget 待配置/无用户时光标不可见 | ✅ | mapping 为空或 username 为空时显示错误提示而非空白 |
| 安静时段 UI 易用性 | ✅ | 标签改为"安静开始(0~23时)"，placeholder 改为"例: 22"，添加助手文字 |

## Git 规范
```
feat(widget): 添加 GitHub 贡献热力图 Widget
fix(widget): 修复 Widget 闪烁问题
fix(widget): 添加 onUpdate 防抖机制
fix(widget): 修复安静时段导致 Widget 卡住问题
fix(worker): 增强 Worker 健壮性，添加超时保护与回退 UI
docs: 更新 README，添加 AI 生成声明
release: v0.1.3
```
分支策略:
- `main` — 稳定版本 (保护) ✅ 已推送
- `dev` — 开发集成分支 (保护) ✅ 已推送
- `feat/github-widget` — 已完成的功能分支 ✅ 已推送
- `feat/<scope>` — 功能分支
- `fix/<scope>` — 修复分支
提交信息使用中文描述 (见 git-convention 技能)

## 推荐库
- 网络: Retrofit + OkHttp, Moshi/Kotlinx Serialization
- SVG: AndroidSVG (com.caverock:androidsvg)
- 异步: Coroutines + Flow, WorkManager
- DI: Hilt | UI: Jetpack Compose, Material Design 3
- 存储: DataStore Preferences

## Get Started

对刚加入此项目的新开发者:

### 项目是什么？
**Dreambox (坠梦, `com.zhuimeng.dreambox`)** — 一个个人 Android 工具箱 App。
当前唯一功能是 **GitHub 贡献热力图桌面 Widget**，支持 4x2 和 2x1 两种尺寸。

### 如何构建和运行
```bash
git clone git@github.com:gozhuimeng/Dream-Box.git
./gradlew assembleDebug   # 构建 Debug APK
./gradlew installDebug    # 安装到设备
```

### 代码架构
```
presentation/  → ui/(Activity, Compose Screens), viewmodel/
domain/        → model/, usecase/, repository/ (接口)
data/          → repository/ (实现), remote/ (API), local/ (存储)
```

### 关键类速览
| 类 | 作用 |
|---|---|
| `GithubWidgetProvider` | 4x2 Widget 生命周期管理 |
| `GithubWidgetTinyProvider` | 2x1 迷你 Widget (继承 4x2) |
| `GithubWidgetWorker` | 后台刷新: 获取 SVG → 渲染 → 更新 RemoteViews |
| `SvgRenderer` | 裁剪/缩放 SVG 适应 Widget 尺寸 |
| `WidgetProfile` | 独立配置项（Profile）数据模型 |
| `WidgetProfileRepository` | Profile 持久化，按 profileId 存储 |
| `WidgetMappingRepository` | appWidgetId ↔ profileId 映射管理 |
| `WidgetConfigureActivity` | Profile 选择器（添加/切换 Widget 时使用） |
| `ProfileViewModel` | 应用内 Profile 管理 |
| `MainActivity` | Compose 主界面，展示 Profile 列表 |

### 关键修复记录
- **闪烁修复**: MIUI 每秒触发 onUpdate → onUpdate 不再修改 UI + 30秒防抖 + UniqueWork KEEP
- **SVG 缩放**: 裁剪左侧标签 + 放大填满 + 右侧对齐
- **跳过配置修复**: 跳过时保存默认配置，Widget 在 App 设置中可见
- **DataStore 清理**: Widget 被删除时自动清理配置数据
- **配置项解耦 (v0.1.3)**: Profile 独立于 Widget，可复用、自由创建/编辑/删除
- **安静时段卡住修复**: 检查移到 "更新中..." 之前，forceRefresh 跳过安静时段
- **Worker 健壮性**: SVG 超时保护(60s)、catch 回退 UI、REPLACE 避免竞态
- **版本**: v0.1.3 (versionCode 4)
