param([string]$Destination)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if (!$Destination) { $Destination=Join-Path $root 'build\release-tools' }
New-Item -ItemType Directory -Force -Path $Destination | Out-Null
$version='1.18.3'
$expected='a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29'
$jar=Join-Path $Destination "bundletool-all-$version.jar"
if (!(Test-Path -LiteralPath $jar)) {
    Invoke-WebRequest -Uri "https://github.com/google/bundletool/releases/download/$version/bundletool-all-$version.jar" -OutFile $jar
}
if ((Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) { throw 'Bundletool checksum mismatch; no unverified tool will be used.' }
Write-Output $jar
