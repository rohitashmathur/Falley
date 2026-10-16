param(
    [string]$SdkRoot=$env:ANDROID_HOME,
    [string]$JdkRoot=$env:JAVA_HOME,
    [string]$BundletoolJar,
    [string]$SigningDirectory,
    [switch]$InitializeSigning
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if (!$SdkRoot -or !$JdkRoot -or !(Test-Path -LiteralPath $BundletoolJar)) { throw 'Pass SDK/JDK 17 paths and an official bundletool-all JAR.' }
$env:JAVA_HOME=$JdkRoot; $env:ANDROID_HOME=$SdkRoot; $env:ANDROID_SDK_ROOT=$SdkRoot
$env:GRADLE_USER_HOME=Join-Path $root 'build\gradle-user-home'
$java=Join-Path $JdkRoot 'bin\java.exe'
$outputs=Join-Path $root 'releases\3.2.0'
New-Item -ItemType Directory -Force -Path $outputs | Out-Null
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
    $aab=Join-Path $outputs 'Falley-3.2.0-beta.aab'
    Copy-Item -LiteralPath (Join-Path $root 'app\build\outputs\bundle\release\app-release.aab') -Destination $aab
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
    if ($xml.manifest.package -ne 'com.perfgames.falley' -or $xml.manifest.GetAttribute('versionCode',$ns.LookupNamespace('android')) -ne '7') { throw 'Wrong release identity/version.' }
    $sdk=$xml.SelectSingleNode('/manifest/uses-sdk')
    if ($sdk.GetAttribute('minSdkVersion',$ns.LookupNamespace('android')) -ne '24' -or $sdk.GetAttribute('targetSdkVersion',$ns.LookupNamespace('android')) -ne '36') { throw 'Unexpected SDK targets in release bundle.' }
    if ($xml.manifest.application.GetAttribute('debuggable',$ns.LookupNamespace('android')) -eq 'true') { throw 'Release bundle is debuggable.' }
    if ($xml.manifest.application.GetAttribute('allowBackup',$ns.LookupNamespace('android')) -ne 'false') { throw 'Offline beta must disable app backup.' }
    if ($xml.SelectNodes('/manifest/uses-permission').Count -ne 0) { throw 'Unexpected requested permissions in offline beta.' }
    $passwordFile=Join-Path $signing.Directory ('bundletool-password-'+[guid]::NewGuid().ToString('N')+'.tmp')
    [IO.File]::WriteAllText($passwordFile,$env:FALLEY_UPLOAD_STORE_PASSWORD,[Text.Encoding]::ASCII)
    $apks=Join-Path $outputs 'Falley-3.2.0-beta-universal.apks'
    Checked $java @('-jar',$BundletoolJar,'build-apks',('--bundle='+$aab),('--output='+$apks),'--mode=universal','--overwrite',('--ks='+$signing.KeyStore),('--ks-key-alias='+$signing.Alias),('--ks-pass=file:'+$passwordFile),('--key-pass=file:'+$passwordFile))
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive=[IO.Compression.ZipFile]::OpenRead($apks)
    $apk=Join-Path $outputs 'Falley-3.2.0-beta-local.apk'
    try { [IO.Compression.ZipFileExtensions]::ExtractToFile($archive.GetEntry('universal.apk'),$apk,$true) }
    finally { $archive.Dispose() }
    Checked (Join-Path $SdkRoot 'build-tools\35.0.0\apksigner.bat') @('verify','--verbose','--print-certs',$apk)
    Checked (Join-Path $SdkRoot 'build-tools\35.0.0\zipalign.exe') @('-c','-P','16','4',$apk)
    Get-FileHash -LiteralPath $aab,$apk -Algorithm SHA256 | ForEach-Object { $_.Hash.ToLowerInvariant()+'  '+[IO.Path]::GetFileName($_.Path) } | Set-Content -LiteralPath (Join-Path $outputs 'SHA256SUMS.txt') -Encoding ascii
    Write-Host "Validated local Play bundle: $aab"
    Write-Host "Local install test APK: $apk"
    Write-Host "Private upload identity preserved in: $($signing.Directory)"
} finally {
    if ($passwordFile -and (Test-Path -LiteralPath $passwordFile)) { Remove-Item -LiteralPath $passwordFile }
    $env:FALLEY_UPLOAD_KEYSTORE=$null; $env:FALLEY_UPLOAD_STORE_PASSWORD=$null
    $env:FALLEY_UPLOAD_KEY_ALIAS=$null; $env:FALLEY_UPLOAD_KEY_PASSWORD=$null
}
