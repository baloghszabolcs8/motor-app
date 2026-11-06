# MotorApp 🏍️

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**MotorApp** is an open-source motorcycle telemetry dashboard and off-grid group ride sharing app built for Android using Jetpack Compose and Kotlin Coroutines/Flow.

Designed specifically for motorcycle handlebar mounting with high visibility, low distraction, and off-grid connectivity.

---

## ✨ Features

- 📐 **Real-Time Lean Angle Meter**: Instant roll angle calculation using device accelerometer low-pass filtering and high-pass noise isolation. Tracks peak left and right lean limits.
- 🚀 **Dynamic G-Force Telemetry**: Live acceleration and braking G-force vector measurement with peak hold tracking.
- 🧭 **Minimalist Black AMOLED HUD Mode**: High-contrast, pure black screen mode displaying extra-large digital time, compass heading, lean angle, and speed with zero clutter.
- 🗺️ **Google Maps Integration**: Live map view showing rider position and heading.
- 📡 **Serverless P2P Group Ride Sharing**: Real-time peer-to-peer location and telemetry sharing using **Google Play Services Nearby Connections API**. Connect with nearby riders off-grid via Bluetooth / Wi-Fi Direct without any server or internet connection.
- 🎯 **One-Tap Calibration**: Simple zero-point tare calibration to align with your handlebar phone mount setup.

---

## 🏗 Architecture & Tech Stack

Built following **Clean Architecture** principles and **MVVM**:

- **Domain Layer**: Pure Kotlin telemetry calculator (`TelemetryCalculator`) without Android SDK dependencies for fast, reliable unit testing.
- **Data Layer**: Reactive sensor & location streams via Kotlin `Flow`, P2P connection handling via `Nearby.getConnectionsClient`.
- **UI Layer**: Modern declarative Jetpack Compose UI with dark mode high-contrast motorcycle dashboard styling.
- **Asynchronous Flow**: Kotlin Coroutines & `StateFlow` for dynamic state observation.

---

## 🛠 Building & Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/motorapp.git
   cd motorapp
   ```

2. **Google Maps API Key**:
   Add your Google Maps API Key in `app/src/main/AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_GOOGLE_MAPS_API_KEY" />
   ```

3. **Build and Run**:
   Open in Android Studio (Ladybug / 2024.2+) and run on a physical Android device (sensors required for lean angle telemetry).

---

## 🧪 Unit Tests

Run unit tests via Gradle:
```bash
./gradlew test
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
