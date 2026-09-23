# Neo Launcher 🚀

A sleek, ultra-responsive, and modern Android launcher built completely from scratch using **Jetpack Compose** and **Material 3 (Material You)**.

Part of the **Neo Deck** ecosystem.

---

## ✨ Features

- 🎨 **Material You Dynamic Theming:**
  - Automatic extraction of dynamic system palette colors from your wallpaper (Monet theming).
  - Support for adaptive monochrome icons with automatic color-harmonization in the App Drawer and on the Home Screen.
  - Dark, Light, and System theme modes.

- 🧩 **Integrated & System Widgets:**
  - **Uhr & Live-Wetter:** Responsive clock and live Open-Meteo weather tile with tap-to-alarm integration.
  - **Akku-Monitor:** Circular battery level indicator and charging status with direct settings shortcut.
  - **Schnellsuche:** Google web search bar with voice recognition intent.
  - **Native System-Widget-Integration:** Custom crash-proof Widget Picker Sheet compatible with all Android OEMs (Xiaomi MIUI/HyperOS, Samsung One UI, Google Pixel).

- 📐 **Interaktive Größenanpassung (Drag-to-Resize):**
  - Intuitive widget resizing in Edit Mode via edge handles (width & height) and corner handles.
  - Live dimensions badge (`4 × 2`, `4 × 1`, etc.) with haptic feedback at grid snap points.
  - Fully responsive widget layouts adapting typography and padding to the selected grid span.

- 📱 **App Drawer & Organisation:**
  - Fast search with instant keyboard focus.
  - Category tabs and hide/unhide app functionality.
  - Support for third-party icon packs (Nova/Lawnchair compatible).
  - App folders on the Home Screen with customizable titles.

- ⚡ **Lightweight & High Performance:**
  - Built with modern 100% Jetpack Compose UI (no legacy XML views).
  - Edge-to-Edge display support with predictive back navigation.
  - Target SDK: **Android 16 (API 36)** | Minimum SDK: **Android 12 (API 31)**.

---

## 🛠️ Tech Stack

- **Language:** Kotlin 2.0+
- **UI Toolkit:** Jetpack Compose & Material 3
- **Architecture:** MVVM (Model-View-ViewModel) + Single Activity
- **Concurrency & Reactivity:** Kotlin Coroutines & StateFlow
- **Toolchain:** Gradle with Foojay JDK 17 resolver

---

## 🚀 Building & Installing

### Prerequisites
- JDK 17+ installed
- Android SDK with platform tools (API 31–36)

### Build Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Install via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
