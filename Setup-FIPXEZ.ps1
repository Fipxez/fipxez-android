$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not (Get-Command py -ErrorAction SilentlyContinue)) { throw 'Python 3 belum terpasang. Install Python 3.11+ lalu ulangi.' }
if (-not (Test-Path "$Root\config.env")) {
  $owner = (& py -c "import secrets; print(secrets.token_urlsafe(36))").Trim()
  $worker = (& py -c "import secrets; print(secrets.token_urlsafe(36))").Trim()
  @("FIPXEZ_OWNER_TOKEN=$owner", "FIPXEZ_WORKER_TOKEN=$worker", "FIPXEZ_API=http://127.0.0.1:8000") | Set-Content -Encoding ascii "$Root\config.env"
  Write-Host 'Token rahasia dibuat di config.env. Jangan bagikan file ini.' -ForegroundColor Green
}
function Read-Config {
  Get-Content "$Root\config.env" | ForEach-Object { if ($_ -match '^([^#=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1],$matches[2],'Process') } }
}
Read-Config
foreach ($part in @('backend','worker')) {
  Push-Location "$Root\$part"
  if (-not (Test-Path '.venv')) { py -m venv .venv }
  & .\.venv\Scripts\python.exe -m pip install --upgrade pip
  & .\.venv\Scripts\python.exe -m pip install -r requirements.txt
  Pop-Location
}
Write-Host "`nSetup selesai. Jalankan .\Run-Backend.ps1 dan .\Run-Worker.ps1 di dua jendela PowerShell." -ForegroundColor Green
if (Get-Command flutter -ErrorAction SilentlyContinue) {
  Push-Location "$Root\android"
  flutter create --platforms=android .
  flutter pub get
  Pop-Location
  Write-Host 'Flutter wrapper Android sudah disiapkan.' -ForegroundColor Green
} else {
  Write-Warning 'Flutter belum terdeteksi. Backend dan worker siap; install Flutter + Android SDK untuk build APK.'
}
