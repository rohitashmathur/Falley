param(
    [Parameter(Mandatory=$true)][string]$DebugConfig,
    [Parameter(Mandatory=$true)][string]$ProductionConfig
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
function Assert-AndroidConfig([string]$Path,[string]$ExpectedPackage) {
    if (!(Test-Path -LiteralPath $Path -PathType Leaf)) { throw 'A supplied Firebase configuration file is missing.' }
    try { $config=Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json }
    catch { throw 'Invalid Firebase Android JSON. Contents are deliberately suppressed.' }
    if ($config.private_key -or $config.type -eq 'service_account') { throw 'Do not use a service-account key. Download the Android app google-services.json instead.' }
    $client=@($config.client | Where-Object { $_.client_info.android_client_info.package_name -eq $ExpectedPackage })
    if ($client.Count -ne 1 -or !$client[0].client_info.mobilesdk_app_id -or !@($client[0].api_key | Where-Object { $_.current_key }).Count) {
        throw "The configuration must contain exactly one matching Android client for $ExpectedPackage. Contents are deliberately suppressed."
    }
}
# Validate both files before replacing either ignored local configuration.
Assert-AndroidConfig $DebugConfig 'com.perfgames.falley.motiontest'
Assert-AndroidConfig $ProductionConfig 'com.perfgames.falley'
foreach ($pair in @(@($DebugConfig,'debug'),@($ProductionConfig,'release'))) {
    $destination=Join-Path $root ('app/src/'+$pair[1]+'/google-services.json')
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
    if ([IO.Path]::GetFullPath($pair[0]) -ne [IO.Path]::GetFullPath($destination)) { Copy-Item -LiteralPath $pair[0] -Destination $destination }
}
Write-Output 'Validated and installed local debug/production Android configs. JSON files and generated Firebase resources must remain out of Git.'
