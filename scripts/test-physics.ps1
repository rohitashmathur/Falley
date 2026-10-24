param([string]$JdkRoot=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
# Keep the established command working as the portable suite grows.
& (Join-Path $PSScriptRoot 'test-expansion.ps1') -JdkRoot $JdkRoot
