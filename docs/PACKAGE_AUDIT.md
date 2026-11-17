# Source package preparation notes

This document records what was changed while preparing the project for a public GitHub repository. It is not a claim that the Android app has passed a full device test.

## Included preparation changes

- Reorganized the uploaded Windows-style ZIP paths into normal Git-compatible directory paths.
- Added direct Gradle dependencies for OkHttp, Coil Compose, USB Serial for Android, Kotlin Coroutines, and Compose extended material icons used by the source imports.
- Aligned Kotlin Gradle Plugin to 2.2.21 with the existing AGP 8.10.1 / Gradle 8.11.1 setup.
- Removed the old Compose compiler extension pin, because the Kotlin Compose Gradle plugin is applied.
- Removed a stale unused sample screen from a different package namespace.
- Added a unit test for serialization round-trip of the persisted `Project` model.
- Added a ProGuard keep rule for the Java-serialized project model.
- Added GitHub Actions build/test workflow, issue templates, security/contribution docs and Git publishing instructions.
- Replaced the previous README with source-accurate documentation and the supplied app screenshots.
- Removed the APK from the original ZIP because it was built in July 2025 and does not represent the current source snapshot.

## Important limitations retained and documented

- The project form includes several board choices, but firmware templates currently cover ESP32 Wi-Fi, ESP32 Bluetooth Classic, and ESP8266 Wi-Fi.
- Bluetooth runtime permission handling must be tested/fixed for modern Android versions before claiming reliable Bluetooth support.
- USB permission grant currently reports the result through a receiver; retry/reconnect behavior needs device testing.
- The app requests broad storage permissions, including `MANAGE_EXTERNAL_STORAGE`. This should be redesigned before Play Store or production distribution.
- Local ESP32 HTTP control uses cleartext HTTP and has no device authentication in the supplied firmware template. Keep it on a trusted LAN.
- Java object serialization is used for project files; model evolution requires a compatibility/migration plan.

## Verification status

- Gradle version catalog parsed successfully as TOML.
- Android XML resources parsed successfully.
- README local image/document references checked.
- The ZIP source tree contains no local.properties, signing keystore, real keystore.properties, or stale APK.
- A full Gradle build could not be run in the preparation environment because the Gradle distribution endpoint could not be reached. The included GitHub Actions workflow is intended to perform `testDebugUnitTest` and `assembleDebug` after the first push.
