# FIPXEZ CPM2 Remote
Android remote + Windows worker for local/offline CPM2 Steam save inspection and backup.

- Connect Android and PC to the same trusted Wi-Fi.
- Run the Windows worker; it prints a private bearer token.
- Enter `http://PC-IP:8765` and the token in the Android app.
- Test connection, inspect the save file, and create a timestamped backup.
- Default save path is a best-effort guess: `%USERPROFILE%\AppData\LocalLow\Olzhas\Car Parking 2\SaveFile.es3`. Set `CPM2_SAVE_PATH` if needed.
- This initial version is read-only. It does not modify Green Dollar/Gold Dollar balances. Save format and integrity must first be validated on a copy.
- No online currency changes, server tampering, anti-cheat bypass, or multiplayer exploitation.

GitHub Actions builds artifacts: `FIPXEZ-CPM2-APK` and `FIPXEZ-CPM2-Windows-Worker`.
Close CPM2 before making backups. Allow the worker through Windows Firewall on Private networks only. Keep the token private.
