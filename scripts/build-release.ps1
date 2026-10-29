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
if (!(Test-Path -LiteralPath (Join-Path $root 'app/src/release/google-services.json'))) { throw 'Missing local production Firebase config. Run scripts/configure-firebase.ps1 first.' }
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
    try {
        $gradleLogFile=Join-Path $root 'build/release-gradle.log'
        & (Join-Path $root 'gradlew.bat') --no-daemon --console=plain :app:lintRelease :app:bundleRelease 2>&1 | Tee-Object -FilePath $gradleLogFile
        if ($LASTEXITCODE -ne 0) { throw 'Release Gradle lint/bundle step failed. Inspect the local build log.' }
        if (Select-String -LiteralPath $gradleLogFile -Pattern 'incompatible version of Kotlin|binary version of its metadata|unsupported Kotlin metadata' -Quiet) { throw 'Lint/R8 could not fully read dependency Kotlin metadata. Use compatible SDK/build-tool versions; do not accept an analysis gap.' }
    }
    finally { Pop-Location }
    $aab=Join-Path $outputs ('Falley-'+$releaseVersion+'-beta.aab')
    Copy-Item -LiteralPath (Join-Path $root 'app\build\outputs\bundle\release\app-release.aab') -Destination $aab
    # R8 mapping must travel with this exact bundle, never a previous version.
    $mappingSource=Join-Path $root 'app\build\outputs\mapping\release\mapping.txt'
    if (!(Test-Path -LiteralPath $mappingSource) -or (Get-Item -LiteralPath $mappingSource).Length -le 0) { throw 'R8 produced no release mapping.txt. Do not submit an obfuscated bundle without its mapping.' }
    $mapping=Join-Path $outputs 'mapping.txt'
    Copy-Item -LiteralPath $mappingSource -Destination $mapping
    $mappingHash=(Get-FileHash -LiteralPath $mapping -Algorithm SHA256).Hash.ToLowerInvariant()
    # Keep the nonblank mapping ID actually injected into this build, not one generated later.
    $mappingIdSource=Join-Path $root 'app/build/crashlytics/release/mappingFileId.txt'
    if (!(Test-Path -LiteralPath $mappingIdSource)) { throw 'Missing Crashlytics release mapping ID.' }
    $mappingIdValue=(Get-Content -LiteralPath $mappingIdSource -Raw).Trim()
    if (!$mappingIdValue -or $mappingIdValue -notmatch '^[a-fA-F0-9-]+$' -or $mappingIdValue.Replace('-','') -match '^0+$') { throw 'Release requires a real nonblank Crashlytics mapping ID for later exact-build deobfuscation.' }
    $crashlyticsDirectory=Join-Path $outputs 'crashlytics'
    New-Item -ItemType Directory -Force -Path $crashlyticsDirectory | Out-Null
    $mappingId=Join-Path $crashlyticsDirectory 'mappingFileId.txt'
    Copy-Item -LiteralPath $mappingIdSource -Destination $mappingId
    $mappingIdHash=(Get-FileHash -LiteralPath $mappingId -Algorithm SHA256).Hash.ToLowerInvariant()
    $firebaseConfigHash=(Get-FileHash -LiteralPath (Join-Path $root 'app/src/release/google-services.json') -Algorithm SHA256).Hash.ToLowerInvariant()
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
    if ($xml.manifest.application.GetAttribute('allowBackup',$ns.LookupNamespace('android')) -ne 'false') { throw 'Falley must disable app backup.' }
    $allowedPermissions=@('android.permission.INTERNET','android.permission.ACCESS_NETWORK_STATE','android.permission.WAKE_LOCK','com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE','com.perfgames.falley.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION')
    $permissions=@($xml.SelectNodes('/manifest/uses-permission') | ForEach-Object { $_.GetAttribute('name',$ns.LookupNamespace('android')) })
    if (@($permissions | Where-Object { $_ -notin $allowedPermissions }).Count) { throw 'Unexpected release permission. Only Firebase network/wake-lock/install-referrer and the app-scoped signature receiver permission are permitted; advertising permissions must be removed.' }
    foreach ($requiredPermission in @('android.permission.INTERNET','android.permission.ACCESS_NETWORK_STATE','android.permission.WAKE_LOCK')) {
        if ($requiredPermission -notin $permissions) { throw ('Missing Firebase permission: '+$requiredPermission) }
    }
    foreach ($flag in @('firebase_data_collection_default_enabled','firebase_analytics_collection_enabled','firebase_crashlytics_collection_enabled','google_analytics_adid_collection_enabled','google_analytics_ssaid_collection_enabled','google_analytics_automatic_screen_reporting_enabled','google_analytics_default_allow_analytics_storage','google_analytics_default_allow_ad_storage','google_analytics_default_allow_ad_user_data','google_analytics_default_allow_ad_personalization_signals')) {
        $node=$xml.SelectSingleNode('/manifest/application/meta-data[@android:name="'+$flag+'"]',$ns)
        if (!$node -or $node.GetAttribute('value',$ns.LookupNamespace('android')) -ne 'false') { throw ('Telemetry must default to opt-out: '+$flag) }
    }
    foreach ($startupProvider in @('com.google.firebase.provider.FirebaseInitProvider','com.google.android.gms.measurement.AppMeasurementContentProvider')) {
        if ($xml.SelectSingleNode('/manifest/application/provider[@android:name="'+$startupProvider+'"]',$ns)) { throw 'Firebase startup must stay deferred to the saved player choice.' }
    }
    $receiverPermission=$xml.SelectSingleNode('/manifest/permission[@android:name="com.perfgames.falley.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"]',$ns)
    if ($receiverPermission -and $receiverPermission.GetAttribute('protectionLevel',$ns.LookupNamespace('android')) -notin @('signature','0x00000002','0x2','2')) { throw 'The app-scoped AndroidX receiver permission must be signature-only.' }
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
    # Confirm the runtime resource ID in the bundle-derived APK matches the retained mapping ID.
    $resourceDump=& (Join-Path $SdkRoot 'build-tools/35.0.0/aapt2.exe') dump resources $apk
    if ($LASTEXITCODE -ne 0) { throw 'Unable to inspect the bundle-derived Crashlytics mapping resource.' }
    $mappingResourceFound=$false
    for ($i=0;$i -lt $resourceDump.Count;$i++) {
        if ($resourceDump[$i] -match 'string/com\.google\.firebase\.crashlytics\.mapping_file_id' -and $i+1 -lt $resourceDump.Count -and $resourceDump[$i+1].Contains($mappingIdValue)) { $mappingResourceFound=$true; break }
    }
    $resourceDump=$null
    if (!$mappingResourceFound) { throw 'The installed APK Crashlytics ID differs from the retained mapping ID. Do not upload or submit mismatched artifacts.' }
    $nativeLibraries=& (Join-Path $PSScriptRoot 'verify-native-alignment.ps1') -Apk $apk
    Get-FileHash -LiteralPath $aab,$apk,$mapping,$mappingId -Algorithm SHA256 | ForEach-Object { $_.Hash.ToLowerInvariant()+'  '+[IO.Path]::GetFileName($_.Path) } | Set-Content -LiteralPath (Join-Path $outputs 'SHA256SUMS.txt') -Encoding ascii
    $artifacts=@()
    foreach ($artifact in @($aab,$apk,$mapping)) {
        $artifacts+=[ordered]@{ file=[IO.Path]::GetFileName($artifact); bytes=(Get-Item -LiteralPath $artifact).Length; sha256=(Get-FileHash -LiteralPath $artifact -Algorithm SHA256).Hash.ToLowerInvariant() }
    }
    $receipt=[ordered]@{
        builtAtUtc=[DateTime]::UtcNow.ToString('o'); package='com.perfgames.falley'; versionName=$releaseVersion; versionCode=[int]$releaseCode
        minSdk=24; targetSdk=36; debuggable=$false; minified=$true; resourcesShrunk=$true
        mapping=[ordered]@{ file='mapping.txt'; sha256=$mappingHash; embeddedEntry=$mappingEntry; embeddedSha256=$embeddedMappingHash; exactMatch=$true }
        r8Reports=$reports; artifacts=$artifacts; permissions=$permissions; nativeLibraries=$nativeLibraries
        firebase=[ordered]@{ analytics=$true; crashlytics=$true; collectionDefault=$false; automaticMappingUpload=$false; configuration='Local release variant; JSON and generated resources excluded from Git'; configurationSha256=$firebaseConfigHash; mappingId=[ordered]@{file='crashlytics/mappingFileId.txt';sha256=$mappingIdHash;matchesInstalledApk=$true} }
        checks=@('lintRelease and bundleRelease succeeded','bundletool validation','strict JAR verification','all payload certificates match preserved upload identity','APK signatures','16 KiB ZIP alignment','embedded mapping matches local mapping','Firebase allowed permissions only and collection defaults disabled','runtime Crashlytics mapping ID matches retained ID','64-bit native ELF 16 KiB load alignment')
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
