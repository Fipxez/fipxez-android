$ErrorActionPreference='Stop'; $Root=Split-Path -Parent $MyInvocation.MyCommand.Path
Get-Content "$Root\config.env" | ForEach-Object { if ($_ -match '^([^#=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1],$matches[2],'Process') } }
$env:FIPXEZ_DB="$Root\backend\fipxez.db"
Set-Location "$Root\backend"
& .\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000
