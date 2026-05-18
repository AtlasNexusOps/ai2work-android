# AI2Work — Android App

Native Android wrapper for [AI2Work](https://ai2work.onrender.com) built with Capacitor.

The app loads the live web app — no rebuild needed when the site updates.

## Quick Start

```bash
git clone https://github.com/AtlasNexusOps/ai2work-android
cd ai2work-android
npm install
npx cap add android
npx cap sync android
```

## Build APK

```bash
cd android
./gradlew assembleDebug
# APK → android/app/build/outputs/apk/debug/app-debug.apk
```

Or open in Android Studio:

```bash
npx cap open android
```

## Structure

```
├── capacitor.config.ts   # Points to ai2work.onrender.com
├── package.json
├── www/                  # Minimal static shell
└── android/              # Generated Android project
```

## Requirements

- Node.js ≥ 20
- Android SDK 34+
- JDK 21
- Gradle 8+

## Related

- [Web app source](https://github.com/AtlasNexusOps/ai-lance)
- [Live site](https://ai2work.onrender.com)
