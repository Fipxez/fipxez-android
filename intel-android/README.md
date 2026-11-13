# FIPXEZ INTEL — Android Edition

Android-only mobile toolkit prototype for authorized, public-source investigations.

## Working modules in this build
- Phone number format checks and indicative Indonesian prefix hints; optional user-initiated web search for public spam/scam reports.
- Nearby Wi-Fi scan using Android scan results and runtime location permission.
- Current device last-known GPS/network location, with a handoff to a map app.
- Authorized CCTV stream handoff for RTSP/HTTP(S) URLs to a compatible player already installed on Android.
- Local evidence notes persisted in app-private preferences, with delete and Android share/export.

## Important limitations
- No private subscriber identity/address lookup, covert tracking, password cracking, or unauthorized camera access.
- CCTV is a player handoff, not an embedded RTSP decoder; playback depends on a compatible installed player and valid camera credentials.
- GPS displays last-known location; it does not run continuous background tracking.
- Wi-Fi scans may be rate-limited by Android and can return stale/empty results.
- Evidence is stored locally without encryption in this prototype; do not store highly sensitive material.
- Public web search opens the browser; the app does not scrape or verify search results.
- Features have been compiled in CI but still require device-level testing on a physical Samsung A22.

## Build
Push to branch `fipxez-intel-android`; GitHub Actions builds a debug APK artifact named `FIPXEZ-INTEL-Android-APK`.

Minimum Android 8.0 (API 26), including Android 12.
