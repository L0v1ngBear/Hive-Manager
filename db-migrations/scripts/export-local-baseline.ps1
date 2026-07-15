param(
    [string]$HostName = "127.0.0.1",
    [int]$Port = 3306,
    [string]$Database = "hive",
    [string]$User = "root",
    [string]$OutputDir = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path (Split-Path -Parent $PSScriptRoot) "baseline"
}

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
}

$mysqldump = Get-Command mysqldump -ErrorAction SilentlyContinue
if (-not $mysqldump) {
    Write-Host "mysqldump was not found. Add the MySQL bin directory to PATH, or export schema manually to: $OutputDir\hive_schema_baseline.sql"
    Write-Host "Export requirement: schema only, UTF-8/utf8mb4, no CREATE DATABASE or USE statements."
    exit 1
}

$outputFile = Join-Path $OutputDir "hive_schema_baseline.sql"

Write-Host "Exporting local schema: ${HostName}:$Port/$Database"
Write-Host "Output file: $outputFile"
Write-Host "Enter the MySQL password when prompted. The password is not stored in this script."

$dumpArgs = @(
    "-h$HostName",
    "-P$Port",
    "-u$User",
    "-p",
    "--no-data",
    "--single-transaction",
    "--routines",
    "--triggers",
    "--events",
    "--default-character-set=utf8mb4",
    "--hex-blob",
    "--skip-comments",
    "--set-gtid-purged=OFF",
    "--result-file=$outputFile",
    $Database
)

& $mysqldump.Source @dumpArgs

if (-not (Test-Path $outputFile) -or ((Get-Item $outputFile).Length -le 0)) {
    throw "Export failed: $outputFile is empty."
}

Write-Host "Export finished: $outputFile"
