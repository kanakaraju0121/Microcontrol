# App builds

No APK is checked into this source repository because the supplied APK was built from an older source snapshot and should not be presented as the current build.

Build a fresh debug APK from the repository root:

```bash
./gradlew assembleDebug
```

On Windows Command Prompt or PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

The output is `app/build/outputs/apk/debug/app-debug.apk`. Do not commit signing keys, keystore passwords, or private release credentials. Publish verified APKs through GitHub Releases after testing them on a device.
