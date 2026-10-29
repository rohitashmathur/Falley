param(
    [Parameter(Mandatory=$true)][string]$ReleaseDirectory,
    [string]$SdkRoot=$env:ANDROID_HOME,
    [string]$JdkRoot=$env:JAVA_HOME,
    [string]$SigningDirectory
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$ReleaseDirectory=[IO.Path]::GetFullPath($ReleaseDirectory)
if (!$SdkRoot -or !$JdkRoot) { throw 'Pass the same SDK/JDK17 paths used to validate the release.' }
$receiptFile=Join-Path $ReleaseDirectory 'release-verification.json'
if (!(Test-Path -LiteralPath $receiptFile)) { throw 'A successful exact-build release-verification.json is required before mapping upload.' }
$receipt=Get-Content -LiteralPath $receiptFile -Raw | ConvertFrom-Json
if ($receipt.package -ne 'com.perfgames.falley' -or !$receipt.minified -or !$receipt.firebase.mappingId.matchesInstalledApk -or !$receipt.mapping.exactMatch) { throw 'The release receipt does not establish a validated production mapping and runtime ID.' }
function Assert-Hash([string]$Path,[string]$Expected) {
    if (!$Expected -or !(Test-Path -LiteralPath $Path) -or (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant() -ne $Expected) { throw 'An exact-build release input changed. Rebuild and validate the release; no mapping will be uploaded.' }
}
$mapping=Join-Path $ReleaseDirectory 'mapping.txt'
$mappingId=Join-Path $ReleaseDirectory $receipt.firebase.mappingId.file
$config=Join-Path $root 'app/src/release/google-services.json'
$currentMapping=Join-Path $root 'app/build/outputs/mapping/release/mapping.txt'
$currentMappingId=Join-Path $root 'app/build/crashlytics/release/mappingFileId.txt'
$currentBundle=Join-Path $root 'app/build/outputs/bundle/release/app-release.aab'
$bundle=@($receipt.artifacts | Where-Object { $_.file.EndsWith('.aab') })
if ($bundle.Count -ne 1) { throw 'Expected exactly one validated production bundle.' }
foreach ($artifact in $receipt.artifacts) { Assert-Hash (Join-Path $ReleaseDirectory $artifact.file) $artifact.sha256 }
Assert-Hash $mapping $receipt.mapping.sha256
Assert-Hash $mappingId $receipt.firebase.mappingId.sha256
Assert-Hash $config $receipt.firebase.configurationSha256
Assert-Hash $currentMapping $receipt.mapping.sha256
Assert-Hash $currentMappingId $receipt.firebase.mappingId.sha256
Assert-Hash $currentBundle $bundle[0].sha256
$env:JAVA_HOME=$JdkRoot; $env:ANDROID_HOME=$SdkRoot; $env:ANDROID_SDK_ROOT=$SdkRoot
$env:GRADLE_USER_HOME=Join-Path $root 'build/gradle-user-home'
try {
    # The Gradle input guard runs again after dependencies and before the upload action.
    $signing=& (Join-Path $PSScriptRoot 'release-signing.ps1') -JdkRoot $JdkRoot -SigningDirectory $SigningDirectory
    Push-Location $root
    try {
        & (Join-Path $root 'gradlew.bat') --no-daemon --console=plain -PfalleyUploadMapping=true ('-PfalleyExpectedMappingSha256='+$receipt.mapping.sha256) ('-PfalleyExpectedMappingIdSha256='+$receipt.firebase.mappingId.sha256) ('-PfalleyExpectedFirebaseConfigSha256='+$receipt.firebase.configurationSha256) ('-PfalleyExpectedBundleSha256='+$bundle[0].sha256) :app:uploadCrashlyticsMappingFileRelease
        if ($LASTEXITCODE -ne 0) { throw 'Crashlytics mapping upload did not complete. Retain the local log and validated release artifacts.' }
    } finally { Pop-Location }
    Assert-Hash $currentMapping $receipt.mapping.sha256
    Assert-Hash $currentMappingId $receipt.firebase.mappingId.sha256
    Assert-Hash $currentBundle $bundle[0].sha256
    $upload=[ordered]@{completedAtUtc=[DateTime]::UtcNow.ToString('o');package=$receipt.package;versionName=$receipt.versionName;versionCode=$receipt.versionCode;mappingSha256=$receipt.mapping.sha256;mappingIdSha256=$receipt.firebase.mappingId.sha256;bundleSha256=$bundle[0].sha256;uploadTask='uploadCrashlyticsMappingFileRelease';taskSucceeded=$true;exactInputsUnchanged=$true;note='Mapping upload task succeeded; Firebase Console symbolication and report arrival need separate confirmation.'}
    $upload | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $ReleaseDirectory 'crashlytics-upload-verification.json') -Encoding utf8
    Write-Output 'Crashlytics exact-build release mapping upload succeeded; local input hashes and runtime mapping ID remain unchanged.'
} finally {
    $env:FALLEY_UPLOAD_KEYSTORE=$null; $env:FALLEY_UPLOAD_STORE_PASSWORD=$null
    $env:FALLEY_UPLOAD_KEY_ALIAS=$null; $env:FALLEY_UPLOAD_KEY_PASSWORD=$null
}
