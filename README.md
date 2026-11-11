# FIPXEZ Hybrid Security Audit 0.3.0

FIPXEZ consists of a Flutter Android control panel, a FastAPI backend for Windows, and a lightweight PC worker. The Android app registers authorized hostnames, queues passive checks, and displays scan status.

## Security scope
- Use only domains you own/manage or for which you have written authorization. Record the permission/ticket reference.
- The worker performs one HTTPS GET to the registered hostname and records selected security headers. It does not crawl, exploit, brute-force, bypass authentication, collect credentials, scan ports, or modify targets.
- Results are configuration observations, not proof of a vulnerability or a penetration test.
- The backend rejects hostnames that resolve to non-public IP addresses. This is a guardrail, not a substitute for authorization.
- Do not expose backend port 8000 to the public internet. For the prototype, use a trusted private Wi-Fi network only. Production use requires HTTPS, secret management, firewall restrictions, backups, and a security review.
- Never commit or share `config.env`; it contains owner and worker tokens.

## Windows 10 setup
1. Extract the repository to a normal folder, for example `C:\\FIPXEZ`.
2. Open PowerShell in that folder. If script execution is blocked, run `Set-ExecutionPolicy -Scope Process Bypass`.
3. Run `./Setup-FIPXEZ.ps1`. This creates random tokens in `config.env`, Python virtual environments, and installs dependencies.
4. Open two PowerShell windows in the project folder. Run `./Run-Backend.ps1` in the first and `./Run-Worker.ps1` in the second.
5. Check `http://127.0.0.1:8000/health` on the PC. API docs are at `http://127.0.0.1:8000/docs`.

## Android connection
1. Connect the phone and PC to the same trusted Wi-Fi.
2. Run `ipconfig` on Windows and note the PC IPv4 address.
3. Allow inbound TCP 8000 in Windows Firewall for Private networks only. Do not configure router port forwarding.
4. Install the APK produced by GitHub Actions.
5. In FIPXEZ, enter `http://IP-PC:8000` and the owner token from `config.env`. The app keeps the token in memory only; it does not persist it.
6. Register an authorized public hostname and its permission/ticket reference, then queue a scan. Keep the worker running on the PC.

## Build APK with GitHub Actions
The workflow `.github/workflows/build-android.yml` runs automatically after relevant source changes and can also be started manually:
1. Open the repository's **Actions** tab.
2. Select **Build FIPXEZ Android APK**.
3. For a manual build, choose **Run workflow** and confirm.
4. Open the completed run and download the **FIPXEZ-Android-APK** artifact.

The cloud workflow runs Python compilation, a backend smoke test, Flutter analysis, Flutter widget tests, and a release APK build. The APK is an unsigned release build for testing/sideloading. A production release requires your own signing keystore. The APK is a control panel, not a hosted cloud service: it requires the Windows backend and worker to be running and reachable over the same network.

## API overview
- `GET /health` — backend health.
- `POST /targets` and `GET /targets` — register/list authorized targets.
- `POST /scans`, `GET /scans`, and `GET /scans/{id}` — queue and inspect checks.
- `GET /scans/{id}/analysis` — local rule-based recommendations.
- `GET /audit` — audit events.
- Worker endpoints use `X-Worker-Token`; owner endpoints use `X-Owner-Token`.

## Requirements
Windows 10 64-bit, Python 3.11+, Flutter stable and Android SDK for local builds. The GitHub Actions build does not require Flutter or Android Studio on the user's PC.
