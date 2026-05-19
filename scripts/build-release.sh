#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."

npm install
npx cap add android 2>/dev/null || true
npx cap sync android

# Kotlin + BouncyCastle
sed -i "/plugins {/a \    id 'org.jetbrains.kotlin.android' version '2.0.21' apply false" android/build.gradle
sed -i "/plugins {/a \    id 'org.jetbrains.kotlin.android'" android/app/build.gradle
sed -i "/dependencies {/a \    implementation 'org.bouncycastle:bcprov-jdk15on:1.70'" android/app/build.gradle

# Copy native wallet sources
NATIVE_DIR="android/app/src/main/java/tech/atlasnexus/ai2work/wallet"
mkdir -p "$NATIVE_DIR"
cp -v native/android/tech/atlasnexus/ai2work/wallet/*.kt "$NATIVE_DIR/"
cp -v native/android/tech/atlasnexus/ai2work/wallet/*.java "$NATIVE_DIR/"

cd android && ./gradlew assembleRelease && cd ..
echo "✅ APK built"
ls -lh android/app/build/outputs/apk/release/app-release-unsigned.apk
