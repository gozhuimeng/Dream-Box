<p align="right">
  <a href="README.md">🌐 中文</a>
</p>

<h1 align="center">Dreambox · 坠梦</h1>

<p align="center">
  <em>Personal Android Toolbox — Starting with a GitHub Contribution Heatmap Widget</em>
</p>

<p align="center">
  <a href="https://github.com/gozhuimeng/Dream-Box/releases">
    <img src="https://img.shields.io/github/v/release/gozhuimeng/Dream-Box" alt="Release">
  </a>
  <img src="https://img.shields.io/badge/license-CC%20BY--NC--SA%204.0-orange" alt="License">
  <img src="https://img.shields.io/badge/minSdk-24-brightgreen" alt="minSdk 24">
  <img src="https://img.shields.io/badge/targetSdk-34-blue" alt="targetSdk 34">
  <img src="https://img.shields.io/badge/Kotlin-2.0-purple" alt="Kotlin 2.0">
</p>

## 📄 License

This work is licensed under a **Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)**.

- ✅ **Allowed** — Personal learning, modification, non-commercial sharing
- ⚠️ **Required** — Attribution to the original author, share derivative works under the same license
- ❌ **Prohibited** — Any commercial use

See the full license text in the [LICENSE](LICENSE) file.

---


**Dreambox (坠梦)** is a personal Android toolbox app. Its first and currently only feature is a **GitHub contribution heatmap desktop widget**.

Place your GitHub contribution graph directly on your home screen — glance at your coding activity without opening your phone. Comes in **4x2** and **2x1** sizes.

> Data sourced from [ghchart.rshah.org](https://ghchart.rshah.org) — **no API key, no GitHub OAuth required**.

---

## ✨ Features

- ✅ **GitHub Contribution Heatmap Widget** — 4x2 standard (username + heatmap + timestamp + refresh button)
- ✅ **2x1 Mini Widget** — Heatmap only, minimal layout
- ✅ **Multi-user Support** — Each widget instance configured independently with a different GitHub username
- ✅ **Custom Colors** — Any 6-digit hex color code
- ✅ **Custom Refresh Interval** — Configurable update frequency
- ✅ **Quiet Hours** — Skip refreshes during nighttime to save battery and data
- ✅ **Dark Theme** — Semi-transparent dark background (`#E61C1B1F`) + 12dp rounded corners
- ✅ **Light / Dark Toggle** — Per-widget theme setting
- ✅ **Manual Refresh** — One-tap immediate update
- ✅ **In-app Management** — View, edit, and delete all configured widgets from the app
- ✅ **MIUI Compatible** — Fixed the flickering issue caused by MIUI's frequent onUpdate calls

---

## 📱 Screenshots

> *(Screenshots coming soon)*

| Widget Type | Light Theme | Dark Theme |
|:---:|:---:|:---:|
| **4x2 Standard** | — | — |
| **2x1 Mini** | — | — |

---

## 🚀 Quick Start

### Download & Install

Download the latest APK from [GitHub Releases](https://github.com/gozhuimeng/Dream-Box/releases) and install it.

### Add a Widget

1. **Long-press** an empty area on your home screen
2. Select **Widgets**
3. Find **Dreambox**
4. Choose **4x2** or **2x1** size and drag it to your home screen
5. Enter a GitHub username in the configuration screen
6. Or **skip configuration** and set it up later in the app

### Manage Widgets

Open the **Dreambox** app to see all configured widgets:

- Tap ✏️ **Edit** — Change username, color, refresh interval, dark theme, etc.
- Tap 🔄 **Refresh** — Update the widget immediately
- Tap 🗑️ **Delete** — Remove the configuration (widget also removed from home screen)

---

## 🔧 Build from Source

```bash
# Clone the repository
git clone git@github.com:gozhuimeng/Dream-Box.git
cd Dream-Box

# Build Debug APK
./gradlew assembleDebug

# Build Release APK (signed with debug keystore)
./gradlew assembleRelease

# Install to device
./gradlew installDebug
```

> **Note**: The Release APK is signed with the debug keystore for convenience. Replace with your own signing config before publishing to any app store.

---

## 🏗️ Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Background Tasks | WorkManager |
| Storage | DataStore Preferences |
| Networking | OkHttp |
| SVG Rendering | AndroidSVG |
| Widget | RemoteViews + AppWidgetProvider |
| Min SDK | Android 7.0 (API 24) |
| Target SDK | Android 14 (API 34) |

---

## 📂 Project Structure

```
app/
├── src/main/
│   ├── java/com/zhuimeng/dreambox/
│   │   ├── data/
│   │   │   ├── GithubChartApi.kt       # SVG data fetching
│   │   │   ├── SvgRenderer.kt          # SVG crop/scale/render
│   │   │   ├── WidgetConfig.kt         # Config data model
│   │   │   └── WidgetConfigRepository.kt # DataStore persistence
│   │   ├── ui/
│   │   │   └── WidgetSettingsViewModel.kt # Settings ViewModel
│   │   ├── widget/
│   │   │   ├── GithubWidgetProvider.kt    # 4x2 Widget Provider
│   │   │   ├── GithubWidgetTinyProvider.kt # 2x1 Widget Provider
│   │   │   ├── GithubWidgetWorker.kt      # Background refresh Worker
│   │   │   └── WidgetConfigureActivity.kt # Configuration Activity
│   │   ├── DreamboxApp.kt               # Application class
│   │   └── MainActivity.kt              # Main UI (Compose)
│   ├── res/
│   │   ├── drawable/
│   │   │   ├── widget_bg_light.xml      # Light rounded background
│   │   │   └── widget_bg_dark.xml       # Dark rounded background
│   │   ├── layout/
│   │   │   ├── github_widget_layout.xml      # 4x2 layout
│   │   │   └── github_widget_layout_tiny.xml # 2x1 layout
│   │   └── xml/
│   │       ├── github_widget_info.xml        # 4x2 metadata
│   │       └── github_widget_tiny_info.xml   # 2x1 metadata
```

---

## ❓ FAQ

### The widget shows "Loading..." or is blank?

- Check your internet connection
- Verify the GitHub username is correct (case-sensitive)
- Both Wi-Fi and mobile data require the `INTERNET` permission
- The first refresh might take up to one cycle after adding

### My widget flickers on MIUI / Xiaomi devices?

This issue was fixed in v0.1.1. If you still experience it, try re-adding the widget from the home screen.

### How do I change the widget color?

Open the Dreambox app → tap ✏️ next to the widget → modify the color (hex format, e.g., `#39D353`).

### Why can't I add a widget from inside the app?

Android widgets must be added from the **home screen** (long-press → Widgets). The "Add Widget" button in the app provides guidance only.

---

## 📄 License

---



<p align="center">
  <sub>Made with ❤️ and Kotlin | Personal project · Continuously evolving</sub>
</p>
