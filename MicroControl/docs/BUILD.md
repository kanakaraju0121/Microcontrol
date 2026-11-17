# Build notes

## Toolchain

- JDK 17
- Android Gradle Plugin 8.10.1
- Gradle Wrapper 8.11.1
- Kotlin 2.2.21
- Android SDK Platform 35

The AGP 8.10 line requires Gradle 8.11.1. Kotlin Gradle Plugin 2.2.21 is within the Kotlin compatibility table for Gradle 8.11.1 and AGP 8.10.1.

## Commands

```bash
./gradlew --version
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Windows: use `gradlew.bat` rather than `./gradlew`.

## Direct application dependencies

- AndroidX / Jetpack Compose and Navigation Compose
- OkHttp for HTTP control requests
- Coil Compose for profile image loading
- USB Serial for Android for USB host serial access
- Kotlin Coroutines for asynchronous work

The USB serial artifact is resolved through JitPack, which is declared in `settings.gradle.kts`.

## Build verification status

Missing direct dependency declarations found during source inspection have been added. A full Gradle build could not be completed in the preparation environment because the Gradle distribution host was unreachable. GitHub Actions is configured to run the tests and debug build after push. Treat a green CI result plus a physical-device smoke test as the publication gate.

## Serialization compatibility

Project records are stored with Java object serialization. The ProGuard rule keeps the serialized `Project` model and its members stable in minified release builds. Avoid renaming/restructuring the persisted model without a migration plan. A versioned JSON or Room-backed schema would be more maintainable for a production release.
