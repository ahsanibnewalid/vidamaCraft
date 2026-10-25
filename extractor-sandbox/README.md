# VidamaCraft Extractor Sandbox

This is a separate Android Gradle project nested in the repository. It is **not included in the production app's Gradle settings**, and the main app does not depend on it. Its yt-dlp Android wrapper dependency therefore does not get added to the production APK unless someone explicitly changes the production project.

## Purpose

- Test wrapper initialization and `getInfo()` metadata mapping in isolation.
- Display title, duration, and up to 30 formats returned by the extractor.
- Measure sandbox APK size independently.
- Keep FFmpeg and actual media downloading out of this first spike.

## Build and run

From the repository root, use the existing Gradle wrapper:

```bash
./gradlew -p extractor-sandbox :app:assembleDebug
```

Install the generated APK on a test device:

```bash
adb install -r extractor-sandbox/app/build/outputs/apk/debug/app-debug.apk
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Test procedure

1. Build and install only the sandbox app.
2. Use the built-in sample URL only if you are authorized to access and test it; replace it with another permitted test URL if unavailable.
3. Tap **Test metadata** and record title, duration, format count, and errors.
4. Record APK size with `ls -lh extractor-sandbox/app/build/outputs/apk/debug/app-debug.apk`.
5. Compare this APK with the production app's baseline APK. These are separate applications, so the comparison measures the experiment's standalone footprint rather than an exact incremental production APK delta.
6. Repeat on supported physical devices/ABIs and record initialization/query latency.

## Important limits

- This is an experimental prototype, not production-ready functionality.
- The dependency version and native ABI compatibility must be verified by a successful build and device test; this commit does not claim those tests have passed.
- No FFmpeg dependency, media download flow, DRM bypass, or production integration is included.
- Review the exact dependency and transitive dependency licenses before distribution. The upstream project's licensing obligations may affect commercial use.
- The sandbox's ABI filters are limited to `armeabi-v7a`, `arm64-v8a`, and `x86_64`; test on the actual target devices before narrowing further.
