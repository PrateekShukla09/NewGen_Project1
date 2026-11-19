# =====================================================================
# Vaccination Drive Scheduler - PowerShell Build and Execution Script
# =====================================================================

param (
    [string]$TargetClass = "com.vaccination.main.Main"
)

$ErrorActionPreference = "Stop"

$ProjectDir = $PSScriptRoot
Set-Location $ProjectDir

$LibJar = "lib/mysql-connector-j-9.2.0.jar"
$Classpath = "bin;$LibJar;config"

Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host "    VACCINATION DRIVE SCHEDULER - POWERSHELL RUNNER" -ForegroundColor Cyan
Write-Host "===================================================================" -ForegroundColor Cyan

if (-not (Test-Path $LibJar)) {
    Write-Error "MySQL Connector/J JAR not found at: $LibJar"
    exit 1
}

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

Write-Host "[1/2] Compiling all Java source files..." -ForegroundColor Yellow
$javaFiles = Get-ChildItem -Path "src" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
& javac -encoding UTF-8 -cp "$LibJar;src" -d bin $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}
Write-Host "[OK] Compilation successful! Output written to bin\" -ForegroundColor Green

Write-Host "`n[2/2] Running $TargetClass..." -ForegroundColor Yellow
Write-Host "===================================================================" -ForegroundColor Cyan
& java -cp $Classpath $TargetClass
