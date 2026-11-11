# FIPXEZ Mobile Edition

FIPXEZ is an Android-first passive HTTPS security-header checker. The Flutter app now runs its check directly on the Android device; it does not need the Windows backend or PC worker.

## Mobile workflow
1. Install the APK from the GitHub Actions artifact.
2. Open FIPXEZ on Android.
3. Enter a hostname you own or are explicitly authorized to assess.
4. Enter the permission/ticket reference for your record.
5. Start the passive check and review the HTTP status and selected response headers.

The mobile engine performs one HTTPS GET request, does not follow redirects, and reads selected security-related response headers. It does not crawl, exploit, brute-force, bypass authentication, collect credentials, scan ports, or modify targets. Results are configuration observations, not proof of a vulnerability or a penetration test.

## Privacy and limitations
- No PC, local backend, PC worker, owner token, or same-Wi-Fi connection is required for the Android mobile check.
- The current session keeps target and result history in app memory; closing the app clears that temporary history.
- The permission/ticket reference is a user-entered record and is not independently verified by the app.
- Only assess systems you own/manage or for which you have explicit written authorization.
- Network access and valid HTTPS/TLS are required.

## Legacy PC components
The `backend/` and `worker/` folders remain in the repository as legacy hybrid components and are not required by the current Android mobile workflow.

## Build APK with GitHub Actions
The workflow `.github/workflows/build-android.yml` runs automatically after relevant source changes and can also be started manually:
1. Open the repository's **Actions** tab.
2. Select **Build FIPXEZ Android APK**.
3. Open the completed run and download the **FIPXEZ-Android-APK** artifact.

The APK is an unsigned release build for testing/sideloading. A production release requires a signing keystore.
