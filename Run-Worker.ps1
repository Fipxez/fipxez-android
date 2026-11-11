$ErrorActionPreference='Stop'; $Root=Split-Path -Parent $MyInvocation.MyCommand.Path
Get-Content "$Root\config.env" | ForEach-Object { if ($_ -match '^([^#=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1],$matches[2],'Process') } }
$env:FIPXEZ_API='http://127.0.0.1:8000'
Set-Location "$Root\worker"
& .\.venv\Scripts\python.exe worker.py
