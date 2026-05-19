#!/bin/bash
# Build release APK with native wallet plugin and upload to GitHub Releases
set -euo pipefail
cd "$(dirname "$0")/.."

npm install
npx cap add android 2>/dev/null || true
npx cap sync android

# Add BouncyCastle dependency for secp256k1 ECDSA
sed -i "/dependencies {/a \    implementation 'org.bouncycastle:bcprov-jdk15on:1.70'" android/app/build.gradle

# Copy native wallet plugin sources
NATIVE_DIR="android/app/src/main/java/tech/atlasnexus/ai2work/wallet"
mkdir -p "$NATIVE_DIR"
cp -v native/android/tech/atlasnexus/ai2work/wallet/*.kt "$NATIVE_DIR/"
cp -v native/android/tech/atlasnexus/ai2work/wallet/*.java "$NATIVE_DIR/"

# Build release APK
cd android
./gradlew assembleRelease
cd ..

APK="android/app/build/outputs/apk/release/app-release-unsigned.apk"
if [ -f "$APK" ]; then
    echo "✅ Release APK built: $APK"
    ls -lh "$APK"
else
    echo "❌ Build failed"
    exit 1
fi
