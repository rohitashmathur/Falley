param(
    [string]$SdkRoot=$env:ANDROID_HOME,
    [string]$JdkRoot=$env:JAVA_HOME,
    [string]$BundletoolJar,
    [string]$SigningDirectory,
    [string]$OutputDirectory,
    [switch]$InitializeSigning
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if (!$SdkRoot -or !$JdkRoot -or !(Test-Path -LiteralPath $BundletoolJar)) { throw 'Pass SDK/JDK 17 paths and an official bundletool-all JAR.' }
$env:JAVA_HOME=$JdkRoot; $env:ANDROID_HOME=$SdkRoot; $env:ANDROID_SDK_ROOT=$SdkRoot
$env:GRADLE_USER_HOME=Join-Path $root 'build\gradle-user-home'
$java=Join-Path $JdkRoot 'bin\java.exe'
$gradleSource=Get-Content -LiteralPath (Join-Path $root 'app\build.gradle') -Raw
if ($gradleSource -notmatch "versionName\s+'([0-9]+\.[0-9]+\.[0-9]+)'" ) { throw 'Expected a numeric release versionName in app/build.gradle.' }
$releaseVersion=$Matches[1]
if ($gradleSource -notmatch 'versionCode\s+(\d+)') { throw 'Expected a release versionCode in app/build.gradle.' }
$releaseCode=$Matches[1]
if ($gradleSource -notmatch 'minifyEnabled\s+(?:=\s*)?true' -or $gradleSource -notmatch 'shrinkResources\s+(?:=\s*)?true') { throw 'Release must enable R8 and resource shrinking in app/build.gradle.' }
$outputs=if ($OutputDirectory) { [IO.Path]::GetFullPath($OutputDirectory) } else { Join-Path $root ('releases\'+$releaseVersion) }
New-Item -ItemType Directory -Force -Path $outputs | Out-Null
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$passwordFile=$null
function Checked([string]$Program,[string[]]$Arguments) {
    & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw ('Release step failed: '+[IO.Path]::GetFileName($Program)+' (exit '+$LASTEXITCODE+')') }
}
try {
    $signing=& (Join-Path $PSScriptRoot 'release-signing.ps1') -Initialize:$InitializeSigning -JdkRoot $JdkRoot -SigningDirectory $SigningDirectory
    Push-Location $root
    try { Checked (Join-Path $root 'gradlew.bat') @('--no-daemon','--console=plain',':app:lintRelease',':app:bundleRelease') }
    finally { Pop-Location }
    $aab=Join-Path $outputs ('Falley-'+$releaseVersion+'-beta.aab')
    Copy-Item -LiteralPath (Join-Path $root 'app\build\outputs\bundle\release\app-release.aab') -Destination $aab
    # R8 mapping must travel with this exact bundle, never a previous version.
    $mappingSource=Join-Path $root 'app\build\outputs\mapping\release\mapping.txt'
    if (!(Test-Path -LiteralPath $mappingSource) -or (Get-Item -LiteralPath $mappingSource).Length -le 0) { throw 'R8 produced no release mapping.txt. Do not submit an obfuscated bundle without its mapping.' }
    $mapping=Join-Path $outputs 'mapping.txt'
    Copy-Item -LiteralPath $mappingSource -Destination $mapping
    $mappingHash=(Get-FileHash -LiteralPath $mapping -Algorithm SHA256).Hash.ToLowerInvariant()
    $mappingEntry='BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map'
    $bundleZip=[IO.Compression.ZipFile]::OpenRead($aab)
    try {
        $entry=$bundleZip.GetEntry($mappingEntry)
        if (!$entry -or $entry.Length -le 0) { throw 'The AAB is missing its embedded R8 mapping. Fix bundle packaging before submitting to Play.' }
        $entryStream=$entry.Open()
        $digest=[Security.Cryptography.SHA256]::Create()
        try { $embeddedMappingHash=[BitConverter]::ToString($digest.ComputeHash($entryStream)).Replace('-','').ToLowerInvariant() }
        finally { $entryStream.Dispose(); $digest.Dispose() }
        if ($embeddedMappingHash -ne $mappingHash) { throw 'The embedded bundle mapping differs from mapping.txt. Keep the mapping generated for this exact AAB.' }
    } finally { $bundleZip.Dispose() }
    $r8=Join-Path $outputs 'r8'
    New-Item -ItemType Directory -Force -Path $r8 | Out-Null
    $reports=@()
    foreach ($name in @('configuration.txt','usage.txt','seeds.txt','resources.txt','missing_rules.txt')) {
        $reportSource=Join-Path (Split-Path -Parent $mappingSource) $name
        if (Test-Path -LiteralPath $reportSource) {
            $reportPath=Join-Path $r8 $name
            Copy-Item -LiteralPath $reportSource -Destination $reportPath
            $reports+= [ordered]@{ file=('r8/'+$name); bytes=(Get-Item -LiteralPath $reportPath).Length; sha256=(Get-FileHash -LiteralPath $reportPath -Algorithm SHA256).Hash.ToLowerInvariant() }
        }
    }
    Checked $java @('-jar',$BundletoolJar,'validate',('--bundle='+$aab))
    Checked (Join-Path $JdkRoot 'bin\jarsigner.exe') @('-verify','-strict','-keystore',$signing.KeyStore,'-storepass:env','FALLEY_UPLOAD_STORE_PASSWORD',$aab,$signing.Alias)
    $verifier=Join-Path $root 'build\release-verifier'
    New-Item -ItemType Directory -Force -Path $verifier | Out-Null
    Checked (Join-Path $JdkRoot 'bin\javac.exe') @('-encoding','UTF-8','-d',$verifier,(Join-Path $PSScriptRoot 'VerifyBundleSignature.java'))
    Checked $java @('-cp',$verifier,'VerifyBundleSignature',$aab,$signing.KeyStore,$signing.Alias)
    $manifest=Join-Path $outputs 'release-manifest.xml'
    & $java -jar $BundletoolJar dump manifest ('--bundle='+$aab) --module=base | Set-Content -LiteralPath $manifest -Encoding utf8
    if ($LASTEXITCODE -ne 0) { throw 'Unable to inspect bundle manifest.' }
    $xml=[xml](Get-Content -LiteralPath $manifest -Raw)
    $ns=New-Object Xml.XmlNamespaceManager($xml.NameTable); $ns.AddNamespace('android','http://schemas.android.com/apk/res/android')
    if ($xml.manifest.package -ne 'com.perfgames.falley' -or $xml.manifest.GetAttribute('versionCode',$ns.LookupNamespace('android')) -ne $releaseCode -or $xml.manifest.GetAttribute('versionName',$ns.LookupNamespace('android')) -ne $releaseVersion) { throw 'Wrong release identity/version.' }
    $sdk=$xml.SelectSingleNode('/manifest/uses-sdk')
    if ($sdk.GetAttribute('minSdkVersion',$ns.LookupNamespace('android')) -ne '24' -or $sdk.GetAttribute('targetSdkVersion',$ns.LookupNamespace('android')) -ne '36') { throw 'Unexpected SDK targets in release bundle.' }
    if ($xml.manifest.application.GetAttribute('debuggable',$ns.LookupNamespace('android')) -eq 'true') { throw 'Release bundle is debuggable.' }
    if ($xml.manifest.application.GetAttribute('allowBackup',$ns.LookupNamespace('android')) -ne 'false') { throw 'Offline beta must disable app backup.' }
    if ($xml.SelectNodes('/manifest/uses-permission').Count -ne 0) { throw 'Unexpected requested permissions in offline beta.' }
    $passwordFile=Join-Path $signing.Directory ('bundletool-password-'+[guid]::NewGuid().ToString('N')+'.tmp')
    [IO.File]::WriteAllText($passwordFile,$env:FALLEY_UPLOAD_STORE_PASSWORD,[Text.Encoding]::ASCII)
    $apks=Join-Path $outputs ('Falley-'+$releaseVersion+'-beta-universal.apks')
    Checked $java @('-jar',$BundletoolJar,'build-apks',('--bundle='+$aab),('--output='+$apks),'--mode=universal','--overwrite',('--ks='+$signing.KeyStore),('--ks-key-alias='+$signing.Alias),('--ks-pass=file:'+$passwordFile),('--key-pass=file:'+$passwordFile))
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive=[IO.Compression.ZipFile]::OpenRead($apks)
    $apk=Join-Path $outputs ('Falley-'+$releaseVersion+'-beta-local.apk')
    try { [IO.Compression.ZipFileExtensions]::ExtractToFile($archive.GetEntry('universal.apk'),$apk,$true) }
    finally { $archive.Dispose() }
    Checked (Join-Path $SdkRoot 'build-tools\35.0.0\apksigner.bat') @('verify','--verbose','--print-certs',$apk)
    Checked (Join-Path $SdkRoot 'build-tools\35.0.0\zipalign.exe') @('-c','-P','16','4',$apk)
    Get-FileHash -LiteralPath $aab,$apk,$mapping -Algorithm SHA256 | ForEach-Object { $_.Hash.ToLowerInvariant()+'  '+[IO.Path]::GetFileName($_.Path) } | Set-Content -LiteralPath (Join-Path $outputs 'SHA256SUMS.txt') -Encoding ascii
    $artifacts=@()
    foreach ($artifact in @($aab,$apk,$mapping)) {
        $artifacts+=[ordered]@{ file=[IO.Path]::GetFileName($artifact); bytes=(Get-Item -LiteralPath $artifact).Length; sha256=(Get-FileHash -LiteralPath $artifact -Algorithm SHA256).Hash.ToLowerInvariant() }
    }
    $receipt=[ordered]@{
        builtAtUtc=[DateTime]::UtcNow.ToString('o'); package='com.perfgames.falley'; versionName=$releaseVersion; versionCode=[int]$releaseCode
        minSdk=24; targetSdk=36; debuggable=$false; minified=$true; resourcesShrunk=$true
        mapping=[ordered]@{ file='mapping.txt'; sha256=$mappingHash; embeddedEntry=$mappingEntry; embeddedSha256=$embeddedMappingHash; exactMatch=$true }
        r8Reports=$reports; artifacts=$artifacts
        checks=@('lintRelease and bundleRelease succeeded','bundletool validation','strict JAR verification','all payload certificates match preserved upload identity','APK signatures','16 KiB ZIP alignment','embedded mapping matches local mapping')
        note='Local build verification only. No Play upload, rollout, website deployment, or physical-device acceptance is implied.'
    }
    $receipt | ConvertTo-Json -Depth 7 | Set-Content -LiteralPath (Join-Path $outputs 'release-verification.json') -Encoding utf8
    Write-Host "R8 mapping retained and matched to embedded bundle metadata: $mapping"
    Write-Host "R8 optimization reports: $r8"
    Write-Host "Validated local Play bundle: $aab"
    Write-Host "Local install test APK: $apk"
    Write-Host "Private upload identity preserved in: $($signing.Directory)"
} finally {
    if ($passwordFile -and (Test-Path -LiteralPath $passwordFile)) { Remove-Item -LiteralPath $passwordFile }
    $env:FALLEY_UPLOAD_KEYSTORE=$null; $env:FALLEY_UPLOAD_STORE_PASSWORD=$null
    $env:FALLEY_UPLOAD_KEY_ALIAS=$null; $env:FALLEY_UPLOAD_KEY_PASSWORD=$null
}
