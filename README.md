# VidamaCraft

VidamaCraft is an Android-first media download manager concept inspired by the convenience of apps such as VidMate and TubeMate. It is being built from scratch with a focus on a clean experience, reliable downloads, privacy, and safe handling of media URLs.

> **Development status: scaffold / early development — not production-ready.** No release APK is available yet. Features must be implemented, built, tested on devices, and security-reviewed before release.

## Planned MVP

- Native Android app (Kotlin + Jetpack Compose)
- Paste a direct media-file URL and choose a local filename
- Download using Android's system `DownloadManager` with system notifications and retry support
- Download history/status and clear error messages
- Dark/light theme and accessible responsive UI
- Input validation, HTTPS-first handling, safe filename handling, and automated CI checks

## Legal and platform boundaries

VidamaCraft is intended for downloading content that the user owns, is authorized to download, or is offered for download by its publisher. It will not bypass DRM, paywalls, authentication controls, or technical restrictions, and will not include tools designed to evade a platform's terms. Website or streaming-page URLs are not necessarily direct downloadable media URLs. Users must respect copyright, privacy, and the terms that apply to each source.

## Planned stack

- Kotlin, Android SDK, Jetpack Compose, Material 3
- Gradle Kotlin DSL
- Android `DownloadManager` for direct file downloads
- GitHub Actions for build and test validation

## Development roadmap

1. Establish the Android project and CI build.
2. Implement and test direct-URL downloads, validation, status, and history.
3. Add unit tests, UI tests, accessibility checks, and network-security review.
4. Verify behavior across supported Android versions and real devices.
5. Prepare privacy policy, license notices, release signing, and distribution documentation.

## Build

Build instructions will be added with the Android project scaffold. A successful CI build is required before any release is described as ready.

## Contributing

Use feature branches and pull requests. Do not commit signing keys, passwords, API tokens, or private user data.
