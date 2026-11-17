# MicroControl validation checklist

Record the app version, Android version, device model and target board when testing. Keep each outcome factual: `Pass`, `Fail`, `Blocked`, or `Not tested`.

## App and project management
- [ ] App installs and launches on Android 7.0+ (API 24+)
- [ ] Create and save a project
- [ ] Close/reopen the app and confirm the project is available
- [ ] Rename and delete a project
- [ ] Export and import a project file
- [ ] Confirm malformed import files are handled safely

## Firmware templates
- [ ] Generate ESP32 Wi-Fi template and compile with the selected ESP32 core
- [ ] Generate ESP32 Bluetooth Classic template and compile it
- [ ] Generate ESP8266 Wi-Fi template and compile it
- [ ] Check generated GPIO assignments against the actual hardware

## Controller transport
- [ ] Wi-Fi: verify `/control` connection test on a trusted LAN
- [ ] Wi-Fi: verify LED, servo and display command handling
- [ ] Bluetooth Classic: grant permissions, connect to an SPP device and send commands
- [ ] USB OTG: grant USB permission, choose baud rate, send and receive serial data
- [ ] Confirm disconnect/reconnect behavior for each mode

## Android permissions and data
- [ ] Test permission flow on Android 10, 12, 13 and 14+ as available
- [ ] Verify project import/export and profile image selection
- [ ] Review broad `MANAGE_EXTERNAL_STORAGE` access before production release

## CI / release
- [ ] GitHub Actions Android CI passes
- [ ] Install the fresh debug APK on a physical phone
- [ ] Run connection tests against a compatible board
- [ ] Record known failures and limitations
- [ ] Publish an APK only after device validation

## Test record template

| Date | App commit | Android/device | Board | Test | Result | Notes |
|---|---|---|---|---|---|---|
| YYYY-MM-DD | commit SHA | model/API | board model | test name | Pass/Fail/Blocked/Not tested | evidence link |
