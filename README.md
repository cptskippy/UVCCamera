# UVCCamera

[![GitHub Repo stars](https://img.shields.io/github/stars/cptskippy/UVCCamera?style=flat&logo=github)](https://github.com/cptskippy/UVCCamera)
[![GitHub License](https://img.shields.io/github/license/cptskippy/UVCCamera)](./LICENSE.md)
[![Maven Central Version](https://img.shields.io/maven-central/v/org.uvccamera/lib)](https://mvnrepository.com/artifact/org.uvccamera/lib)
[![Pub Version](https://img.shields.io/pub/v/uvccamera)](https://pub.dev/packages/uvccamera)

A USB Video Class (UVC) camera library for Android and a plugin for Flutter.

This project is a hard fork of the original [UVCCamera by saki4510t](https://github.com/saki4510t/UVCCamera) and is
brought to you by [Alexey Pelykh](https://alexey-pelykh.com) with a great gratitude to the original project's
author [saki4510t](https://github.com/saki4510t/) and its community of contributors. It includes some improvements from
the original project's forks and PRs.

## 16 KB Page Size Support

This fork adds full support for Android devices with **16 KB memory pages** (Android 15+), ensuring compatibility with
next-generation hardware such as Pixel 10 and other devices shipping with 16 KB page sizes.

### What changed

- **Linker flag**: `APP_LDFLAGS := -Wl,-z,max-page-size=16384` in [`Application.mk`](lib/src/main/jni/Application.mk)
  forces all native libraries (`libUVCCamera`, `libuvc`, `libusb`, `libjpeg`) to align LOAD segments to 16 KB boundaries.
- **NDK r28+**: The build requires NDK `28.0.13004108` or later, which defaults to 16 KB page alignment.
- **Platform bump**: `APP_PLATFORM` raised from `android-21` to `android-26` for modern NDK compatibility.

### Supported ABIs

| ABI | Status |
|-----|--------|
| `armeabi-v7a` | ✅ Supported |
| `arm64-v8a` | ✅ Supported |

### Minimum Android API level

- **Native libraries**: API 26+ (Android 8.0) due to `APP_PLATFORM := android-26`
- **Library minSdk**: API 21 (Android 5.0) — note that native code requires API 26+ at runtime

## Usage

### Android library

The library is available on Maven Central and GitHub Packages.

#### Maven Central

Add the following dependency to your `build.gradle` file:

```groovy
implementation 'org.uvccamera:lib:0.1.0'
```

Or to your `build.gradle.kts` file:

```kotlin
implementation("org.uvccamera:lib:0.1.0")
```

#### GitHub Packages

For the latest builds including 16 KB support, use GitHub Packages:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/cptskippy/UVCCamera")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// build.gradle.kts
implementation("org.uvccamera:lib:<version>")
```

> **Note**: GitHub Packages requires authentication. For private repos, generate a [personal access token](https://github.com/settings/tokens) with `read:packages` scope and set it as `gpr.key` in your `gradle.properties`.

### Flutter plugin

The Flutter plugin is available on [pub.dev](https://pub.dev/packages/uvccamera). To use it in your Flutter project, add
the following dependency:

```yaml
dependencies:
  uvccamera: ^0.1.0
```

See the [Flutter example](https://pub.dev/packages/uvccamera/example) for an app that uses the plugin.

## Building Locally

### Prerequisites

- **JDK 17** (Temurin recommended)
- **Android SDK** with Build Tools and platform SDK
- **NDK r28+** (`28.0.13004108` or later)

### Setup NDK

```shell
sdkmanager "ndk;28.0.13004108"
```

### Build native libraries

```shell
cd lib/src/main/jni
ndk-build -j$(nproc)
```

### Build AAR

```shell
./gradlew :lib:assembleRelease
```

### Publish to local Maven

Required before building the Flutter plugin example:

```shell
./gradlew :lib:publishToMavenLocal
```

### Build Flutter plugin example

```shell
cd flutter/example
flutter build apk
```

### Full build chain

```shell
./gradlew assembleRelease publishToMavenLocal && cd flutter/example && flutter build apk
```

## Verification

Verify that your `.so` files are 16 KB aligned:

```shell
# Check a single library
readelf -lL libUVCCamera.so | grep LOAD

# Expected output shows alignment of 0x4000 (16384 bytes):
# LOAD           0x000000 0x00000000 0x00000000 0x00000 0x00000 R E 0x4000

# Check all libraries in the output directory
for so in lib/build/intermediates/cxx/RelWithDebInfo/*/obj/*.so; do
  echo "=== $(basename $so) ==="
  readelf -lL "$so" | grep LOAD
done
```

Every `LOAD` segment should show `Align: 0x4000` (16 KB). If you see `Align: 0x200000` (2 MB) or `Align: 0x1000` (4 KB),
the library is not properly aligned for 16 KB devices.

## Development & Contribution

This section describes how to build the Android library and the Flutter plugin from the source code locally.

### Commit conventions

Format: `(type) scope: description`

| Type | Meaning |
|------|---------|
| `fix` | Bug fix |
| `imp` | Improvement/enhancement |
| `chore` | Maintenance (deps, CI, tooling) |
| `docs` | Documentation |

Scope prefix when targeting a specific module: `flutter:`, `ci:`, `lib:`

### Branch naming

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feat/description` | `feat/flutter/pause-resume-preview` |
| Fix | `fix/description` | `fix/preview-size-comparison` |

## License

The original license applies to the relevant parts of this project as well:

> Copyright (c) 2014-2017 saki t_saki@serenegiant.com
>
> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at
>
>     http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.
>
> All files in the folder are under this Apache License, Version 2.0.
> Files in the jni/libjpeg, jni/libusb and jin/libuvc folders may have a different license,
> see the respective files.

Some dependencies may have different licenses, so please check the dependencies' licenses before using this project.

## Upstreams

Most of the contributions picked from the original project's forks and PRs are attributed to the respective authors in
the commit messages and stored or referenced in the [upstreams](./upstreams) folder.
