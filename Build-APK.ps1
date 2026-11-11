$ErrorActionPreference='Stop'; $Root=Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not (Get-Command flutter -ErrorAction SilentlyContinue)) { throw 'Flutter SDK tidak ditemukan. Install Flutter + Android SDK, tutup dan buka PowerShell, lalu ulangi.' }
Set-Location "$Root\android"
flutter create --platforms=android --org id.fipxez .
flutter pub get
flutter analyze
$manifest="$Root\android\app\src\main\AndroidManifest.xml"
if (Test-Path $manifest) {
  $xml=Get-Content $manifest -Raw
  if ($xml -notmatch 'android.permission.INTERNET') { $xml=$xml -replace '<application',('<uses-permission android:name="android.permission.INTERNET" />' + [Environment]::NewLine + '    <application') }
  if ($xml -notmatch 'usesCleartextTraffic') { $xml=$xml -replace '<application','<application android:usesCleartextTraffic="true"' }
  Set-Content -Path $manifest -Value $xml -Encoding utf8
}
flutter build apk --release
Write-Host "APK: $Root\android\build\app\outputs\flutter-apk\app-release.apk" -ForegroundColor Green
