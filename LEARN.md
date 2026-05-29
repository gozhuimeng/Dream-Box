# Android 开发学习指南

## 环境配置

### 1. Android SDK (通过 sdkman 管理)

```bash
# 安装Android SDK
sdk install android-sdk "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

# 或手动配置环境变量 (添加到 ~/.bashrc)
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

source ~/.bashrc

# 安装SDK组件
sdk install android-sdk  # 如果用sdkman管理
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

验证:
```bash
echo $ANDROID_HOME
sdkmanager --list_installed
```

### 2. Nvim LSP 配置 (Kotlin/Android)

你已用 mason，添加 Kotlin LSP 只需修改 `lua/plugins/mason.lua`:

```lua
-- 在 servers 列表中添加:
kotlin_language_server = {},

-- 注意: Kotlin LSP 需要手动下载
-- mkdir -p ~/.local/share/nvim/mason/packages/kotlin-language-server
-- wget https://github.com/fwcd/kotlin-language-server/releases/download/xxx/kotlin-language-server.zip
-- unzip kotlin-language-server.zip
```

重启后运行 `:MasonInstall kotlin_language_server`

**推荐按键 (已配置 lspsaga):**
```bash
gd          # 跳转到定义
K           # 查看文档/提示
gr          # 查找引用
gh          # 查看诊断 (等同于 lspsaga)
<leader>ca  # 代码动作
```

---

## 学习路线图 (6-8周)

### 第一阶段: Kotlin基础 (1-2周)

**目标**: 掌握Kotlin语法，能写简单程序

| 内容 | 说明 |
|------|------|
| 变量、类型 | `val`/`var`、基本类型、类型推断 |
| 函数 | 函数定义、默认参数、命名参数 |
| 控制流 | `if/else`、`when`、循环 |
| 类与对象 | 类、构造器、继承 |
| 空安全 | `?.`、`?:`、`lateinit`、`by lazy` |
| 集合 | `List`、`Map`、`filter`、`map` |
| Lambda | 匿名函数、高阶函数 |

**练习**: 写一个四则运算计算器

**资源**: [Kotlin官方教程](https://kotlinlang.org/docs/getting-started.html)

---

### 第二阶段: Android基础 (2-3周)

**目标**: 理解Android四大组件，能开发简单App

| 内容 | 说明 |
|------|------|
| Activity | 生命周期 (`onCreate`/`onStart`/`onResume` 等)、Intent跳转 |
| Layout | XML布局、ConstraintLayout、常用控件 |
| ViewModel | 生命周期感知、数据持久化 |
| Fragment | FragmentManager、Navigation |
| RecyclerView | 列表展示、Adapter模式 |

**练习**: 做一个待办事项App (Todo List)

---

### 第三阶段: 网络与数据 (1-2周)

**目标**: 能调用API、存储数据

| 内容 | 工具 |
|------|------|
| 网络请求 | Retrofit + OkHttp |
| JSON解析 | Moshi 或 Kotlinx Serialization |
| 本地存储 | Room 数据库 |
| 协程 | Coroutines + Flow |

**练习**: 调用 GitHub API 展示用户仓库列表

---

### 第四阶段: Widget开发 (1周)

**目标**: 能开发桌面组件

| 内容 | 说明 |
|------|------|
| AppWidgetProvider | Widget生命周期 |
| RemoteViews | 简单UI展示 |
| WorkManager | 后台数据更新 |

**最终项目**: GitHub贡献热力图Widget

---

## 项目结构

```
myapp/
├── app/
│   ├── src/main/
│   │   ├── java/com/myapp/
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/          # 数据层
│   │   │   ├── domain/        # 业务逻辑
│   │   │   ├── presentation/ # UI层
│   │   │   └── widget/        # Widget
│   │   └── res/
│   │       ├── layout/
│   │       └── xml/
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 常用命令

```bash
# 构建
./gradlew assembleDebug      # Debug APK
./gradlew installDebug        # 安装到设备
./gradlew test                # 测试

# SDK
sdkmanager --list            # 列出可用组件
sdkmanager --install "xxx"    # 安装组件
```

---

## 推荐库

| 类别 | 库 |
|------|-----|
| 网络 | Retrofit + OkHttp, Moshi |
| 异步 | Coroutines + Flow |
| DI | Hilt |
| UI | Jetpack Compose, Material Design 3 |
| 存储 | Room, DataStore |
| 后台 | WorkManager |
