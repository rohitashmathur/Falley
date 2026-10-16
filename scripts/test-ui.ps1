param(
    [switch]$BuildOnly,
    [switch]$RestoreOnly,
    [switch]$EffectsOnly,
    [switch]$V31Only,
    [switch]$ReleaseOnly,
    [switch]$StoreOnly,
    [switch]$ReleaseSigning,
    [string]$SdkRoot=$env:ANDROID_HOME,
    [string]$JdkRoot=$env:JAVA_HOME,
    [string]$Device='emulator-5580',
    [ValidateSet('small','tall','store')][string[]]$Viewport=@('small','tall'),
    [string]$ArtifactRoot,
    [string]$KeyStore
)
$ErrorActionPreference='Stop'
if ($BuildOnly -and $RestoreOnly) { throw 'Choose BuildOnly or RestoreOnly.' }
if (@($EffectsOnly,$V31Only,$ReleaseOnly,$StoreOnly | Where-Object { $_ }).Count -gt 1) { throw 'Choose one focused or store mode.' }
if ($ReleaseSigning -and $KeyStore) { throw 'ReleaseSigning loads the existing upload identity; do not also pass KeyStore.' }
if ($StoreOnly) { $Viewport=@('store') }
if (!$StoreOnly -and $Viewport -contains 'store') { throw 'The store viewport requires StoreOnly.' }
$root=Split-Path -Parent $PSScriptRoot
$frontlineRoot=Split-Path -Parent (Split-Path -Parent $root)
if (!$SdkRoot) { $SdkRoot=Join-Path $frontlineRoot '.toolchain\android-sdk' }
if (!$JdkRoot) { $JdkRoot=(Get-ChildItem -LiteralPath (Join-Path $frontlineRoot '.toolchain') -Directory -Filter 'jdk-*' | Select-Object -First 1).FullName }
if (!$ArtifactRoot) { $ArtifactRoot=Join-Path $root $(if ($StoreOnly) { 'build\store-captures' } elseif ($ReleaseOnly) { 'build\device\v32-focused' } elseif ($V31Only) { 'build\device\v32-v31-focused' } elseif ($EffectsOnly) { 'build\device\v32-effects' } else { 'build\device\v32' }) }
$output=Join-Path $root 'build\android-ui-falley'
$apk=Join-Path $output $(if ($ReleaseSigning) { 'falley-ui-tests-release.apk' } else { 'falley-ui-tests.apk' })
function Checked([string]$Program,[string[]]$Arguments) {
    & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Program failed: $LASTEXITCODE" }
}
try {
if ($ReleaseSigning) {
    $signing=& (Join-Path $PSScriptRoot 'release-signing.ps1') -JdkRoot $JdkRoot
    $KeyStore=$signing.KeyStore
    $signArguments=@('--ks',$KeyStore,'--ks-key-alias',$signing.Alias,'--ks-pass','env:FALLEY_UPLOAD_STORE_PASSWORD','--key-pass','env:FALLEY_UPLOAD_KEY_PASSWORD')
} else {
    if (!$KeyStore) { $KeyStore=Join-Path $root 'build\debug.keystore' }
    $signArguments=@('--ks',$KeyStore,'--ks-pass','pass:android','--key-pass','pass:android')
}
if (!$RestoreOnly) {
    if (!(Test-Path -LiteralPath $KeyStore)) { throw 'Build the app first so its debug signing key exists, or pass -KeyStore.' }
    $env:JAVA_HOME=$JdkRoot
    $buildTools=Join-Path $SdkRoot 'build-tools\35.0.0'
    $androidJar=Join-Path $SdkRoot 'platforms\android-35\android.jar'
    # Never reuse generated class directories: obsolete inner classes cannot enter this APK.
    $staging=Join-Path $output ('staging-'+[guid]::NewGuid().ToString('N'))
    $classes=Join-Path $staging 'classes'
    $dex=Join-Path $staging 'dex'
    New-Item -ItemType Directory -Force -Path $classes,$dex | Out-Null
    $unsigned=Join-Path $staging 'unsigned.apk'
    Checked (Join-Path $buildTools 'aapt.exe') @('package','-f','-M',(Join-Path $root 'android-tests\AndroidManifest.xml'),'-I',$androidJar,'-F',$unsigned)
    # Reflection-only harness: no game source or implementation copies on the classpath.
    Checked (Join-Path $JdkRoot 'bin\javac.exe') @('-encoding','UTF-8','-source','8','-target','8','-classpath',$androidJar,'-d',$classes,(Join-Path $root 'android-tests\com\perfgames\falley\tests\NativeFalleyTest.java'))
    $jar=Join-Path $staging 'classes.jar'
    Checked (Join-Path $JdkRoot 'bin\jar.exe') @('cf',$jar,'-C',$classes,'.')
    Checked (Join-Path $buildTools 'd8.bat') @('--lib',$androidJar,'--min-api','24','--output',$dex,$jar)
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive=[IO.Compression.ZipFile]::Open($unsigned,[IO.Compression.ZipArchiveMode]::Update)
    try { [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,(Join-Path $dex 'classes.dex'),'classes.dex') | Out-Null }
    finally { $archive.Dispose() }
    $aligned=Join-Path $staging 'aligned.apk'
    Checked (Join-Path $buildTools 'zipalign.exe') @('-f','4',$unsigned,$aligned)
    Checked (Join-Path $buildTools 'apksigner.bat') (@('sign')+$signArguments+@('--out',$apk,$aligned))
    Checked (Join-Path $buildTools 'apksigner.bat') @('verify',$apk)
    if ($BuildOnly) { Write-Host "Built native UI harness: $apk"; return }
}
$adb=Join-Path $SdkRoot 'platform-tools\adb.exe'
function Adb([string[]]$Arguments) {
    $result=& $adb -s $Device @Arguments
    if ($LASTEXITCODE -ne 0) { throw "ADB failed on ${Device}: $Arguments" }
    return $result
}
if ($Device -notmatch '^emulator-\d+$' -or (Adb @('get-state')) -ne 'device' -or (Adb @('shell','getprop','ro.kernel.qemu')) -ne '1') { throw 'This fixture only runs on an Android emulator, never a physical phone.' }
if ((Adb @('shell','pm','path','com.perfgames.falley')) -notmatch '^package:') { throw 'Install the Falley app APK separately first.' }
New-Item -ItemType Directory -Force -Path $ArtifactRoot | Out-Null
$component='com.perfgames.falley.tests/com.perfgames.falley.tests.NativeFalleyTest'
function Instrument([string]$Mode,[string]$Shot) {
    $result=Adb @('shell','am','instrument','-r','-w','-e','mode',$Mode,'-e','shot',$Shot,$component)
    $result | Write-Output
    $result | Set-Content -LiteralPath (Join-Path $ArtifactRoot "$Shot-$Mode.txt") -Encoding utf8
    $text=$result -join "`n"
    if ($text -notmatch 'INSTRUMENTATION_CODE: -1' -or $text -notmatch "PASS: native Falley $Mode;" -or $text -match 'FAIL:') { throw "Native Falley $Mode failed for $Shot." }
}
if ($RestoreOnly) { Instrument 'restore' 'recovery'; return }
Adb @('install','-r','-t',$apk) | Out-Null
$size=(Adb @('shell','wm','size')) -join "`n"
$density=(Adb @('shell','wm','density')) -join "`n"
$oldSize=if ($size -match 'Override size: (\d+x\d+)') { $Matches[1] } else { 'reset' }
$oldDensity=if ($density -match 'Override density: (\d+)') { $Matches[1] } else { 'reset' }
$failure=$null
$restored=$false
try {
    foreach ($name in $Viewport) {
        $dimensions=if ($name -eq 'small') { @('480x800','240') } elseif ($name -eq 'store') { @('1080x1920','420') } else { @('720x1600','320') }
        Adb @('shell','am','force-stop','com.perfgames.falley') | Out-Null
        Adb @('shell','wm','size',$dimensions[0]) | Out-Null
        Adb @('shell','wm','density',$dimensions[1]) | Out-Null
        Start-Sleep -Milliseconds 650
        try { Instrument $(if ($StoreOnly) { 'store' } elseif ($ReleaseOnly) { 'release' } elseif ($V31Only) { 'v31' } elseif ($EffectsOnly) { 'effects' } else { 'suite' }) $name }
        finally {
            if ($StoreOnly) {
                foreach ($file in @('campaign.png','landing.png','lightning.png','saw.png','arcade.png','settings.png','sources.json','checks.txt')) {
                    Adb @('pull',"/sdcard/Android/data/com.perfgames.falley/files/falley-native-v32/store/$file",(Join-Path $ArtifactRoot $file)) | Out-Null
                }
            } else { Adb @('pull',"/sdcard/Android/data/com.perfgames.falley/files/falley-native-v32/$name",$ArtifactRoot) | Out-Null }
        }
    }
} catch { $failure=$_ }
finally {
    try { Adb @('shell','am','force-stop','com.perfgames.falley') | Out-Null; Instrument 'restore' 'recovery'; $restored=$true }
    catch { Write-Warning "Preference recovery failed; retain test harness and run -RestoreOnly: $_"; if (!$failure) { $failure=$_ } }
    try { Adb @('shell','wm','size',$oldSize) | Out-Null; Adb @('shell','wm','density',$oldDensity) | Out-Null }
    catch { Write-Warning "Display recovery failed: $_"; if (!$failure) { $failure=$_ } }
    if ($restored) { try { Adb @('uninstall','com.perfgames.falley.tests') | Out-Null } catch { if (!$failure) { $failure=$_ } } }
}
if ($failure) { throw $failure }
if ($StoreOnly) { Write-Host "PASS: Falley V3.2 native 1080x1920 store captures and measured crop metadata. Artifacts: $ArtifactRoot" }
elseif ($ReleaseOnly) { Write-Host "PASS: Falley V3.2 focused release privacy/accessibility/awake, hazards and audio. Audibility not assessed. Artifacts: $ArtifactRoot" }
elseif ($V31Only) { Write-Host "PASS: Falley V3.1 focused five-hazard migration, saw visuals/collision and SoundPool counters/lifecycle. Audibility not assessed. Artifacts: $ArtifactRoot" }
elseif ($EffectsOnly) { Write-Host "PASS: Falley focused fire/three-beam warning, collision and early/late visual fixtures. Artifacts: $ArtifactRoot" }
else { Write-Host "PASS: Falley V3.2 full release privacy/accessibility/awake, Arcade/touch/campaign, saw physics and SoundPool lifecycle. Audibility not assessed. Artifacts: $ArtifactRoot" }
} finally {
    if ($ReleaseSigning) {
        $env:FALLEY_UPLOAD_STORE_PASSWORD=$null
        $env:FALLEY_UPLOAD_KEY_PASSWORD=$null
        $env:FALLEY_UPLOAD_KEYSTORE=$null
        $env:FALLEY_UPLOAD_KEY_ALIAS=$null
    }
}
