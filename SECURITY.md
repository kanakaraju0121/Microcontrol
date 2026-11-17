# Security policy

## Scope

The current `main` branch is the version tracked for security fixes. This is an educational project, not a production-hardened device-management platform.

## Reporting

Do not publish credentials, signing materials, personal data or exploitable security details in a public issue. Contact the repository owner privately through GitHub to coordinate a report.

## Security considerations

- The generated ESP32 example uses HTTP on a local network and does not provide TLS or device authentication.
- The Android manifest declares broad external-storage access. Redesign this before Play Store or production distribution.
- Bluetooth and USB device access must be tested on actual phones and adapters.
- Do not use this prototype to control safety-critical or hazardous equipment.

- Project data currently uses Java object serialization. Import data only from sources you trust; consider replacing this with a versioned, validated data format before broader distribution.
