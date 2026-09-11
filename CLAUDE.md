# UVCCamera

> USB Video Class (UVC) camera library for Android + Flutter plugin

## Project Context

| Attribute | Value |
|-----------|-------|
| **License** | Apache-2.0 |
| **Min SDK** | 21 (Android 5.0) |
| **Compile SDK** | 34 (Android 14) |
| **Java Compatibility** | 11 (build requires JDK 17) |
| **Target ABIs** | armeabi-v7a, arm64-v8a |
| **Gradle** | 8.13, Kotlin DSL, version catalog |
| **Flutter** | >=3.24.0, Dart SDK >=3.5.0 <4.0.0 |
| **Distribution** | Maven Central (`com.github.cptskippy.uvccamera:lib`), GitHub Packages |

## Structure

| Path | Purpose |
|------|---------|
| `lib/` | Android library module (`com.github.cptskippy.uvccamera.lib`) |
| `lib/src/main/java/` | Java API (UVCCamera, USBMonitor, DeviceFilter) |
| `lib/src/main/jni/` | Native C/C++ (libuvc, libusb, libjpeg, UVCCamera JNI) |
| `samples/flutter/` | Flutter plugin reference (`uvccamera`) |
| `samples/flutter/lib/` | Dart API |
| `samples/flutter/android/` | Flutter Android platform implementation (Java) |
| `samples/flutter/example/` | Flutter example app |
| `samples/usbCameraCommon/` | Shared Android UI utilities for test apps |
| `samples/usbCameraTest*/` | Android test/demo applications |
| `gradle/libs.versions.toml` | Dependency version catalog |

## Conventions

### Commits

Format: `(type) scope: description`

| Type | Meaning |
|------|---------|
| `fix` | Bug fix |
| `imp` | Improvement/enhancement |
| `chore` | Maintenance (deps, CI, tooling) |
| `docs` | Documentation |

Scope prefix when targeting a specific module: `flutter:`, `ci:`, `lib:`

Cherry-picked commits use trailers:
```
Cherry-picked-from: source/repo@sha (or source/repo#PR)
Co-authored-by: Original Author <email>
```

### Branches

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feat/description` | `feat/flutter/pause-resume-preview` |
| Fix | `fix/description` | `fix/preview-size-comparison` |
| Cherry-pick | `cherry-pick/source-description` | `cherry-pick/hthetiot-fix-rotation` |

### Naming

- Java packages: `com.serenegiant.usb` / `com.serenegiant.utils` (legacy upstream)
- Library namespace: `com.github.cptskippy.uvccamera.lib`
- Flutter plugin package: `com.github.cptskippy.uvccamera.flutter`
- Dart files: `uvccamera_*.dart` (snake_case with prefix)
- Gradle modules: camelCase (`usbCameraTest`, `usbCameraCommon`)

### Code Style

Per `.editorconfig`: 4-space indent, 120 char max, LF line endings, UTF-8. YAML uses 2-space indent.

## Development

### Build Android library

```shell
./gradlew :lib:assembleRelease
```

### Publish to local Maven (required before Flutter build)

```shell
./gradlew :lib:publishToMavenLocal
```

### Build Flutter example

```shell
cd samples/flutter/example
flutter build apk
```

### Run full build chain

```shell
./gradlew assembleRelease publishToMavenLocal && cd samples/flutter/example && flutter build apk
```

## Release

Tag-triggered: push a semantic version tag (with `v` prefix) to trigger the release workflow.

```shell
git tag vX.Y.Z
git push origin vX.Y.Z
```

The CI workflow automatically publishes to Maven Central and GitHub Packages.

## CI

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `ci.yaml` | Push to main, PRs to main, `workflow_dispatch` | Build lib, verify 16 KB page alignment, publish snapshots |
| `release.yaml` | Tag push (`vX.Y.Z`), `workflow_dispatch` | Full release: build, sign, publish all artifacts |

Snapshot versions: `main-SNAPSHOT` (main branch), `PR-{N}-SNAPSHOT` (pull requests from same repo).

## Native Layer

The native code uses ndk-build (not CMake). Entry point: `lib/src/main/jni/Android.mk`.
NDK version: r28 (`ndk;28.0.13004108`). Built `.so` artifacts are verified for 16 KB page alignment in CI.

Key native libraries:
- **libuvc** — USB Video Class protocol implementation
- **libusb** — USB device access
- **libjpeg-turbo** — JPEG encoding/decoding (SIMD-accelerated)
- **rapidjson** — JSON parsing for device descriptors
- **UVCCamera** — JNI bridge (`serenegiant_usb_UVCCamera.cpp`) and pipeline system

The pipeline architecture (`lib/src/main/jni/UVCCamera/pipeline/`) handles frame processing with buffered, preview, capture, callback, and other specialized pipelines.

Artifacts are published via JReleaser for Maven Central and Gradle publishing plugins for GitHub Packages.

## Fork Context

Hard fork of `saki4510t/UVCCamera`. Community improvements are cherry-picked with attribution trailers.
