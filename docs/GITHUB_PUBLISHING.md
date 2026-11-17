# Publish MicroControl to GitHub

## Suggested repository details

**Repository name**

`MicroControl`

**Description**

`Android microcontroller project builder and controller built with Kotlin and Jetpack Compose, with ESP32/ESP8266 firmware templates and Wi-Fi, Bluetooth Classic and USB serial workflows.`

**Topics**

`android` `kotlin` `jetpack-compose` `esp32` `esp8266` `iot` `embedded-systems` `microcontroller` `bluetooth` `usb-serial`

## Push from Windows Git Bash

1. Create an empty GitHub repository named `MicroControl` under `kanakaraju0121`. Do not initialize it with a README, license or `.gitignore` because those are already in this project.
2. Extract the ZIP.
3. Open Git Bash in the extracted `MicroControl` folder (the folder containing `settings.gradle.kts`).
4. Run the following commands one at a time:

```bash
git init -b main
git add .
git status
git commit -m "feat: publish MicroControl Android IoT application"
git remote add origin https://github.com/kanakaraju0121/MicroControl.git
git push -u origin main
```

5. Open the repository on GitHub and confirm screenshots and the README render correctly.
6. Open **Actions** and check the Android CI workflow. A green workflow means the hosted Gradle tests and debug build completed.

## Before you push

- Confirm `.gitignore` excludes `local.properties`, `keystore.properties` and keystores.
- Do not add Wi-Fi passwords, device tokens or personal data.
- The original APK is intentionally not included because it predates the source snapshot. Build a fresh APK and validate on a phone before publishing a release.
- Review the final `git status` before the first commit.
