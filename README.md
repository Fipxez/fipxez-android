# FIPXEZ CPM2 Remote (offline save utility)

Android remote controller + Windows PC worker for **local/offline save inspection and backup** for Car Parking Multiplayer 2 (Steam).

## Current scope
- Android app connects to the Windows worker over the same trusted Wi-Fi.
- Worker health check, save-file discovery, SHA-256/size/modified-time inspection.
- Create a timestamped backup without modifying the original save.
- Default save path is a best-effort guess: `%USERPROFILE%\AppData\LocalLow\Olzhas\Car Parking 2\SaveFile.es3`. Set `CPM2_SAVE_PATH` if the actual location differs.
- The tool does not edit balances yet. Save format, integrity checks, and offline behavior must be validated on a copied save before any local editor can be implemented.
- No online currency changes, server tampering, anti-cheat bypass, or multiplayer exploitation.

## Android build
GitHub Actions builds the APK when this branch is pushed. Open **Actions → Build FIPXEZ CPM2 APK** and download artifact `FIPXEZ-CPM2-APK`.

## Windows worker
The workflow also packages `worker/worker.py` as `FIPXEZ-CPM2-Worker.exe` using PyInstaller on Windows. Run the EXE; allow it through Windows Firewall on **Private networks only**. The worker prints its bearer token once started. Keep it private.

## Connect
1. Connect Android and PC to the same trusted Wi-Fi.
2. Run the Windows worker EXE.
3. Find the PC's IPv4 address with `ipconfig` and enter `http://PC-IP:8765` in the Android app.
4. Enter the token printed by the worker.
5. Test connection, inspect save, then create a backup.

## Safety
Close CPM2 before backing up. The worker only reads the save and copies it to Documents/FIPXEZ-CPM2-Backups. It does not write to the game's save path. Use only in offline/single-player contexts and preserve an untouched backup.
