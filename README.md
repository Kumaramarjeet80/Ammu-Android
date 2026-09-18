# Ammu - Pure Offline Native Android Audio Player 🎧

> **Replicating the complete "Ammu" web audio player experience down to every micro-interaction.**
> Built with 100% Offline-First Architecture in **Kotlin**, **Jetpack Compose (Material 3)**, **Media3 ExoPlayer**, and **Room Database**.

---

## 🌟 Key Features

### 1. Audio Engine & DSP
- **Media3 ExoPlayer & MediaSessionService**: Background playback, wake locks (`WAKE_MODE_LOCAL`), automatic audio focus handling, Bluetooth/headset media buttons, and lock-screen notification controls.
- **Dual DSP Equalizer**:
  - **VLC Mode (10-band)**: 60Hz, 170Hz, 310Hz, 600Hz, 1kHz, 3kHz, 6kHz, 12kHz, 14kHz, 16kHz + Bass Boost + Preamp.
  - **Vivo Studio Mode (10-band)**: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz with 11 official presets (`custom`, `pop`, `dance`, `blues`, `classical`, `jazz`, `slow`, `electro`, `rock`, `country`, `close`).
  - **Dynamic Bezier Spline Curve**: Real-time cubic Bezier curve rendering on Canvas.
  - **Auto-Gain Reduction**: 20% master volume automatically applied when DSP is engaged to prevent digital clipping; restores to 100% on bypass.
- **Seamless Crossfade**: Linear ramp volume fading between tracks.
- **A-B Looper**: Live section looping between two custom timestamps.

### 2. Micro-Interactions & Gestures
- **Vinyl Disc Mode**: Tap album artwork to transform into a spinning vinyl record with concentric grooves and spindle hole. Pauses smoothly with playback.
- **Double-Tap Seeking**: Double-tap left half (-10s) or right half (+10s) of artwork with floating feedback overlays (`⏪ -10s` / `⏩ +10s`).
- **Waveform Scrubber & Ticks**: Live waveform canvas with timestamp marker tick pips and active chapter pill badge.
- **Two-Zone Queue Partition**:
  - **Zone A (Drag & Drop)**: Long-press to drag and reorder songs with a red drop indicator line and floating preview card.
  - **Zone B (Action Buttons)**: Shift Up (▲), Shift Down (▼), Remove (✕) scroll-only zone without accidental drag swaps.
- **Reactions & Feedback**: Heart burst particle overlay and floating emotional toast banner ("Thank you for loving me 🥺" / "Dil tod diya na mera 😿 - From Amarjeet").
- **Mini-Player Bar**: Swipe left/right for next/previous track; swipe up to expand to Fullscreen Player Sheet.
- **5-Second Undo Toast**: Full undo support for track deletions and batch removals.

### 3. Security, Storage & Utilities
- **4-Key Security Suite**:
  - 👑 **Master Key**: Unlocks all capabilities.
  - 🔐 **AES-256-GCM Key**: Complete JSON backup encryption (PBKDF2 key derivation, 16-byte salt, 12-byte IV).
  - 🔑 **Creator Passkey**: Protects original creator attribution and playlist metadata.
  - 🎵 **Download Key**: Restricts export and download permissions.
- **Admin Super-Key Recovery**: Account-level super-key in `EncryptedSharedPreferences` with protection: *"There is no super access. You have to put keys to get access."*
- **Storage Matcher**: Scans local storage against imported JSON backups and displays green `✓ Available` and red `✕ Missing from Storage` badges.
- **Storage Auditor & Duplicate Cleaner**: Scans internal app storage blobs for identical size/hash signatures and purges duplicates.
- **Real MP3 Audio Trimmer**: Slices audio streams via `MediaExtractor` and `MediaMuxer` to produce compressed `.mp3` audio clips.

---

## 🏗 Architecture

```
app/src/main/java/com/ammu/player/
├── AmmuApplication.kt
├── audio/
│   ├── AmmuMediaService.kt        # MediaSessionService, ExoPlayer, Audio Focus, WakeLock
│   ├── AudioPlaybackManager.kt    # StateFlow player state machine, queue, sleep timer
│   ├── DspEngine.kt               # VLC & Vivo 10-band Equalizer, Bass Boost, Auto-Gain
│   └── Mp3AudioTrimmer.kt         # MediaExtractor + MediaMuxer audio slicer
├── crypto/
│   ├── SecuritySuite.kt           # AES-256-GCM, PBKDF2, SHA-256, EncryptedSharedPreferences
│   └── BackupManager.kt           # 4-Key Scoped Selective Export & Storage Matcher Import
├── data/
│   ├── local/
│   │   ├── AmmuDatabase.kt        # Room Database v1 with pre-seeding
│   │   ├── dao/                   # Track, Playlist, Favorite, Lyrics, Timestamp, Clip, Audit, Stats
│   │   └── entity/                # 10 Room Entities
│   └── repository/
│       ├── AmmuRepository.kt      # Unified Data Layer, Smart Playlists, Title Clutter Cleaner
│       └── StorageAuditor.kt      # Storage scanner & duplicate cleaner
└── ui/
    ├── MainActivity.kt
    ├── components/
    │   ├── DualEqualizerView.kt   # VLC/Vivo modes with Bezier curve Canvas
    │   ├── EmotionalToast.kt      # Floating card reaction
    │   ├── HeartBurstOverlay.kt   # Particle burst animation
    │   ├── MiniPlayerBar.kt       # Bottom bar with swipe gestures
    │   ├── QueueTwoZoneList.kt    # Zone A (drag-drop) + Zone B (shift/delete)
    │   ├── VinylDiscView.kt       # Spinning vinyl disc with grooves
    │   └── WaveformScrubber.kt    # Waveform canvas with timestamp ticks
    ├── dialogs/                   # Export/Import, Storage Auditor, Trimmer, Insights, Admin Auth
    ├── screens/
    │   ├── AmmuMainScreen.kt      # Main Shell, Chips, Search/Sort, Multi-select, Song List
    │   └── FullscreenPlayerSheet.kt # Pull-to-dismiss sheet, Hero, Sticky Controls, Drawers
    └── theme/                     # Pure AMOLED Midnight Black (#000000) & Material 3
```

---

## 🚀 Building & Running

### Requirements
- Android Studio Ladybug (2024.2+) or newer
- JDK 17
- Android SDK 35 (minSdk 26)

### Build Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 👨‍💻 Developer Attribution
Made with ❤️ by & for **Amarjeet kumar**
- Lead Audio Architect & Systems Engineer
- Pure Offline Audio Processing
