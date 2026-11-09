# MC Player - Advanced Android Video Player

An advanced, high-performance offline video player for Android built with Kotlin, Jetpack Compose, Material Design 3, and Media3 ExoPlayer. Inspired by MX Player, MC Player combines full gesture controls, customizable SRT/VTT subtitle rendering, and an offline media library manager.

---

## 🌟 Key Features

### 👆 Advanced Gesture Controls
- **Left Vertical Swipe:** Instant screen brightness adjustment (0%–100%) with a floating sun HUD.
- **Right Vertical Swipe:** Media stream volume adjustment with **Audio Boost** mode up to 150%.
- **Horizontal Swipe:** Precision scrub seeking with real-time target timestamp and delta indicators (`[+00:30]` / `[-00:15]`).
- **Three-Zone Double-Tap:**
  - Double-tap Left: Rewind -10s (accumulates -10s, -20s, -30s with ripple animation).
  - Double-tap Center: Toggle Play / Pause.
  - Double-tap Right: Fast-forward +10s (accumulates +10s, +20s, +30s).
- **Long-Press 2X Speed Boost:** Press and hold anywhere on screen for instant **2.0x playback speed** with a translucent, compact top badge; release restores original speed.
- **Pinch-to-Zoom & Pan:** Magnify video view from 100% up to 300% with smooth two-finger panning.
- **Screen Lock:** Prevents accidental touches and gestures while watching.

### 📝 Subtitle Engine (SRT & WebVTT)
- **Built-in Parser:** Synchronizes `.srt` and `.vtt` cues in real-time.
- **Live Sync Timing Stepper:** Adjust subtitle offset (`-5s` to `+5s` in 200ms increments) to fix desynchronized audio.
- **Custom Styling:** Configurable font sizes, text colors (White, Yellow, Cyan), high-contrast outlines, and background opacity.
- **External Subtitles:** Load external `.srt` / `.vtt` files directly from device storage.

### 📁 Offline Library & History
- **Device Video Indexer:** Scans local videos with folder categorization, resolution badges, file size, and duration.
- **Showcase Demos:** Includes bundled high-definition offline sample videos with subtitles.
- **Resume Playback:** Room database stores exact watch timestamps, progress bars, and favorites.
- **Aspect Ratio Modes:** Fit, Fill/Crop, Zoom, 16:9, 4:3, and native Android Picture-in-Picture (PiP).

---

## 🛠️ Architecture & Tech Stack

- **Language:** Kotlin 2.2+
- **UI Framework:** Jetpack Compose (Material 3)
- **Media Engine:** AndroidX Media3 ExoPlayer 1.5.1
- **Persistence:** AndroidX Room 2.7.0
- **Image Loading:** Coil Compose
- **Architecture:** MVVM (Model-View-ViewModel) with StateFlow

---

## 🚀 Building & Running

1. Clone this repository:
   ```bash
   git clone <repo-url>
   cd <repo-folder>
   ```
2. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Install onto connected Android device or emulator:
   ```bash
   ./gradlew installDebug
   ```
