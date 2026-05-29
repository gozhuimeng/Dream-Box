# Plan: GitHub 贡献热力图 Widget (v0.1.x ✅ 已归档)

> 本计划已完成。所有功能已开发完成并发布 v0.1.3。下一阶段开发请参考下方"下一阶段"。

## 已完成的功能

| 版本 | 内容 |
|------|------|
| v0.1.0 | 项目搭建 + Widget 核心功能 (Provider/SVG/DataStore/Worker) + 配置界面 |
| v0.1.1 | 修复 MIUI Widget 闪烁问题 (onUpdate 防抖、UniqueWork KEEP) |
| v0.1.2 | 2x1 迷你 Widget、暗色主题/圆角、SVG 缩放、质量修复 |
| v0.1.3 | 配置项解耦 (Profile)、Worker 健壮性增强、安静时段优化、文档完善 |

## 当前版本

- **版本**: v0.1.3 (versionCode 4)
- **Release**: <https://github.com/gozhuimeng/Dream-Box/releases/tag/v0.1.3>
- **APK**: Dream Box-0.1.3-release.apk

## 项目结构 (v0.1.3)

```
app/
├── src/main/
│   ├── java/com/zhuimeng/dreambox/
│   │   ├── data/
│   │   │   ├── GithubChartApi.kt               # SVG 数据获取
│   │   │   ├── SvgRenderer.kt                  # SVG 裁剪/缩放/渲染
│   │   │   ├── WidgetProfile.kt                # Profile 数据模型
│   │   │   ├── WidgetProfileRepository.kt      # Profile 持久化
│   │   │   ├── WidgetMappingRepository.kt      # appWidgetId ↔ profileId 映射
│   │   │   └── MigrationHelper.kt              # 旧版配置 → Profile 迁移
│   │   ├── ui/
│   │   │   └── ProfileViewModel.kt             # Profile 管理 ViewModel
│   │   ├── widget/
│   │   │   ├── GithubWidgetProvider.kt          # 4x2 Widget Provider
│   │   │   ├── GithubWidgetTinyProvider.kt      # 2x1 Widget Provider
│   │   │   ├── GithubWidgetWorker.kt            # 后台刷新 Worker
│   │   │   └── WidgetConfigureActivity.kt       # Profile 选择器
│   │   ├── DreamboxApp.kt                      # Application (Hilt + WorkManager)
│   │   └── MainActivity.kt                     # 主界面 (Compose Profile 列表)
│   ├── res/
│   │   ├── drawable/   # widget_bg_light/dark.xml
│   │   ├── layout/     # github_widget_layout{,_tiny}.xml
│   │   └── xml/        # github_widget_info{,_tiny}.xml
```

## 关键修复记录

- **闪烁修复**: MIUI 每秒触发 onUpdate → onUpdate 不再修改 UI + 30秒防抖 + UniqueWork KEEP
- **SVG 缩放**: 裁剪左侧标签 + 放大填满 + 右侧对齐
- **跳过配置修复**: 跳过时保存默认配置，Widget 在 App 设置中可见
- **DataStore 清理**: Widget 被删除时自动清理配置数据
- **配置项解耦 (v0.1.3)**: Profile 独立于 Widget，可复用、自由创建/编辑/删除
- **安静时段卡住修复**: 检查移到 "更新中..." 之前，forceRefresh 跳过安静时段
- **Worker 健壮性**: SVG 超时保护(60s)、catch 回退 UI、REPLACE 避免竞态

## 下一阶段

> 🚧 **v0.2.x — 待规划**
>
> 当前 Widget 功能已完善。下次开发将进入 v0.2.x 阶段，具体内容待讨论。
>
> 可能的扩展方向：
> - 新的 Widget 类型（如天气、日程、倒计时等）
> - 新的 App 功能模块
> - 架构层面的优化
