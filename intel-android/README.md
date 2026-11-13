# FIPXEZ INTEL — Android Edition

Android-only mobile investigation toolkit prototype.

## Build
Push to branch `fipxez-intel-android` or run the workflow manually:
**Actions → Build FIPXEZ INTEL Android APK**.
Download artifact `FIPXEZ-INTEL-Android-APK`.

## Current MVP
- Dark navy / neon-blue Android dashboard (Jetpack Compose).
- Phone number format analysis only; no private subscriber lookup.
- Nearby Wi-Fi scan using Android's location permission and OS-provided scan results.
- Placeholder modules for device location, authorized CCTV integration, and evidence management.

## Boundaries
The app does not break Wi-Fi passwords, track third-party phones by number, retrieve private home addresses, or access cameras without credentials/authorization. Future integrations must use devices and systems the operator is authorized to access.

## Compatibility
Minimum Android 8.0 (API 26); target API 32 for initial compatibility testing, including Android 12.
