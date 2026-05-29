# AGENTS.md - Android Project

## Active Plans
- [PLAN.md](./PLAN.md) - GitHub 贡献热力图 Widget 开发计划

## 项目信息
- 项目名: **dreambox** (坠梦)
- 包名: `com.zhuimeng.dreambox`
- 定位: 个人工具箱 App
- 最低SDK: 24 / 目标SDK: 34
- 当前状态: GitHub 贡献热力图 Widget 功能已实现，APK 构建通过
- 开发分支: `feat/github-widget`

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

## Git 规范
```
feat(widget): 添加 GitHub 贡献热力图 Widget
fix(widget): 修复 Widget 刷新超时问题
```
分支策略:
- `main` — 稳定版本 (保护)
- `dev` — 开发集成分支 (保护)
- `feat/<scope>` — 功能分支
- `fix/<scope>` — 修复分支
提交信息使用中文描述 (见 git-convention 技能)

## 推荐库
- 网络: Retrofit + OkHttp, Moshi/Kotlinx Serialization
- SVG: AndroidSVG (com.caverock:androidsvg)
- 异步: Coroutines + Flow, WorkManager
- DI: Hilt | UI: Jetpack Compose, Material Design 3
- 存储: DataStore Preferences
