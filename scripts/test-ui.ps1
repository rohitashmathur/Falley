param(
    [switch]$BuildOnly,
    [switch]$RestoreOnly,
    [switch]$EffectsOnly,
    [switch]$V31Only,
    [switch]$ReleaseOnly,
    [switch]$PrivacyOnly,
    [switch]$StoreOnly,
    [switch]$MotionOnly,
    [switch]$ProgressionOnly,
    [switch]$FeatureOnly,
    [switch]$SplashOnly,
    [switch]$RevisionOnly,
    [switch]$RevisionSmoke,
    [switch]$ExpansionOnly,
    [switch]$UxOnly,
    [switch]$V43Only,
    [switch]$FirebaseOnly,
    [switch]$ReleaseSigning,
    [string]$SdkRoot=$env:ANDROID_HOME,
    [string]$JdkRoot=$env:JAVA_HOME,
    [ValidatePattern('^emulator-\d+$')][string]$Device='emulator-5580',
    [ValidatePattern('^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$')][string]$AppPackage='com.perfgames.falley',
    [ValidateSet('small','tall','tablet','xlarge','landscape','native','store')][string[]]$Viewport=@('small','tall'),
    [string]$ArtifactRoot,
    [string]$KeyStore,
    [ValidateRange(0.85,1.6)][double]$FontScale=1
)
$ErrorActionPreference='Stop'
if ($BuildOnly -and $RestoreOnly) { throw 'Choose BuildOnly or RestoreOnly.' }
if (@($EffectsOnly,$V31Only,$ReleaseOnly,$PrivacyOnly,$StoreOnly,$MotionOnly,$ProgressionOnly,$FeatureOnly,$SplashOnly,$RevisionOnly,$RevisionSmoke,$ExpansionOnly,$UxOnly,$V43Only,$FirebaseOnly | Where-Object { $_ }).Count -gt 1) { throw 'Choose one focused or store mode.' }
# Current default is the 4.3 debug suite. Explicit older modes retain version-specific fixtures.
if (@($EffectsOnly,$V31Only,$ReleaseOnly,$PrivacyOnly,$StoreOnly,$MotionOnly,$ProgressionOnly,$FeatureOnly,$SplashOnly,$RevisionOnly,$RevisionSmoke,$ExpansionOnly,$UxOnly,$V43Only,$FirebaseOnly | Where-Object { $_ }).Count -eq 0 -and !$ReleaseSigning) { $V43Only=$true }
if (($MotionOnly -or $ProgressionOnly -or $FeatureOnly -or $SplashOnly -or $RevisionOnly -or $ExpansionOnly -or $UxOnly -or $V43Only -or $FirebaseOnly) -and $ReleaseSigning) { throw 'This focused check is debug-only; use RevisionSmoke for release verification.' }
if ($SplashOnly -and !$PSBoundParameters.ContainsKey('Viewport')) { $Viewport=@('small') }
if ($ReleaseSigning -and $KeyStore) { throw 'ReleaseSigning loads the existing upload identity; do not also pass KeyStore.' }
if ($StoreOnly) { $Viewport=@('store') }
if (!$StoreOnly -and $Viewport -contains 'store') { throw 'The store viewport requires StoreOnly.' }
$root=Split-Path -Parent $PSScriptRoot
$frontlineRoot=Split-Path -Parent (Split-Path -Parent $root)
if (!$SdkRoot) { $SdkRoot=Join-Path $frontlineRoot '.toolchain\android-sdk' }
if (!$JdkRoot) { $JdkRoot=(Get-ChildItem -LiteralPath (Join-Path $frontlineRoot '.toolchain') -Directory -Filter 'jdk-*' | Select-Object -First 1).FullName }
if (!$ArtifactRoot) { $ArtifactRoot=Join-Path $root $(if ($FirebaseOnly) { 'build\device\firebase44' } elseif ($V43Only) { 'build\device\v43' } elseif ($UxOnly) { 'build\device\ux' } elseif ($ExpansionOnly) { 'build\device\expansion' } elseif ($PrivacyOnly) { 'build\device\privacy-contact' } elseif ($RevisionOnly -or $RevisionSmoke) { 'build\device\revision' } elseif ($SplashOnly) { 'build\device\splash' } elseif ($FeatureOnly) { 'build\device\features' } elseif ($ProgressionOnly) { 'build\device\progression' } elseif ($MotionOnly) { 'build\device\motion-fix' } elseif ($StoreOnly) { 'build\store-captures' } elseif ($ReleaseOnly) { 'build\device\v32-focused' } elseif ($V31Only) { 'build\device\v32-v31-focused' } elseif ($EffectsOnly) { 'build\device\v32-effects' } else { 'build\device\v32' }) }
$nativeFolder=if ($FirebaseOnly) { 'falley-native-firebase' } elseif ($V43Only) { 'falley-native-v43' } elseif ($UxOnly) { 'falley-native-ux' } elseif ($ExpansionOnly) { 'falley-native-expansion' } elseif ($PrivacyOnly) { 'falley-native-privacy' } elseif ($RevisionOnly -or $RevisionSmoke) { 'falley-native-revision' } elseif ($SplashOnly) { 'falley-native-splash' } elseif ($FeatureOnly) { 'falley-native-features' } elseif ($ProgressionOnly) { 'falley-native-progression' } elseif ($MotionOnly) { 'falley-native-motion' } else { 'falley-native-v32' }
$output=Join-Path $root ('build\android-ui-falley\'+$Device)
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
    $testManifest=Join-Path $staging 'AndroidManifest.xml'
    [xml]$testManifestXml=Get-Content -LiteralPath (Join-Path $root 'android-tests\AndroidManifest.xml') -Raw
    $testManifestXml.manifest.instrumentation.SetAttribute('targetPackage','http://schemas.android.com/apk/res/android',$AppPackage) | Out-Null
    $testManifestXml.Save($testManifest)
    Checked (Join-Path $buildTools 'aapt.exe') @('package','-f','-M',$testManifest,'-I',$androidJar,'-F',$unsigned)
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
if ((Adb @('shell','pm','path',$AppPackage)) -notmatch '^package:') { throw 'Install the requested Falley app APK separately first.' }
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
$fontScaleChanged=$PSBoundParameters.ContainsKey('FontScale')
$oldFontScale=if ($fontScaleChanged) { (Adb @('shell','settings','get','system','font_scale')).Trim() } else { $null }
$failure=$null
$restored=$false
try {
    foreach ($name in $Viewport) {
        $dimensions=if ($name -eq 'small') { @('480x800','240') } elseif ($name -eq 'store') { @('1080x1920','420') } elseif ($name -eq 'tablet') { @('1200x1920','240') } elseif ($name -eq 'xlarge') { @('1600x2560','240') } elseif ($name -eq 'landscape') { @('1920x1200','240') } else { @('720x1600','320') }
        Adb @('shell','am','force-stop',$AppPackage) | Out-Null
        # Native preserves a cold-boot AVD's real display/taskbar configuration.
        if ($name -ne 'native') {
            Adb @('shell','wm','size',$dimensions[0]) | Out-Null
            Adb @('shell','wm','density',$dimensions[1]) | Out-Null
        }
        if ($fontScaleChanged) { Adb @('shell','settings','put','system','font_scale',$FontScale.ToString([Globalization.CultureInfo]::InvariantCulture)) | Out-Null }
        Start-Sleep -Milliseconds 650
        try { Instrument $(if ($FirebaseOnly) { 'firebase' } elseif ($V43Only) { 'v43' } elseif ($UxOnly) { 'ux' } elseif ($ExpansionOnly) { 'expansion' } elseif ($PrivacyOnly) { 'privacy' } elseif ($RevisionSmoke) { 'revision_smoke' } elseif ($RevisionOnly) { 'revision' } elseif ($SplashOnly) { 'splash' } elseif ($FeatureOnly) { 'features' } elseif ($ProgressionOnly) { 'progression' } elseif ($MotionOnly) { 'motion' } elseif ($StoreOnly) { 'store' } elseif ($ReleaseOnly) { 'release' } elseif ($V31Only) { 'v31' } elseif ($EffectsOnly) { 'effects' } else { 'suite' }) $name }
        finally {
            if ($StoreOnly) {
                foreach ($file in @('campaign.png','landing.png','lightning.png','saw.png','arcade.png','settings.png','sources.json','checks.txt')) {
                    Adb @('pull',"/sdcard/Android/data/$AppPackage/files/$nativeFolder/store/$file",(Join-Path $ArtifactRoot $file)) | Out-Null
                }
            } else { Adb @('pull',"/sdcard/Android/data/$AppPackage/files/$nativeFolder/$name",$ArtifactRoot) | Out-Null }
        }
    }
} catch { $failure=$_ }
finally {
    try { Adb @('shell','am','force-stop',$AppPackage) | Out-Null; Instrument 'restore' 'recovery'; $restored=$true }
    catch { Write-Warning "Preference recovery failed; retain test harness and run -RestoreOnly: $_"; if (!$failure) { $failure=$_ } }
    try { Adb @('shell','wm','size',$oldSize) | Out-Null; Adb @('shell','wm','density',$oldDensity) | Out-Null }
    catch { Write-Warning "Display recovery failed: $_"; if (!$failure) { $failure=$_ } }
    if ($fontScaleChanged) {
        try {
            if (!$oldFontScale -or $oldFontScale -eq 'null') { Adb @('shell','settings','delete','system','font_scale') | Out-Null }
            else { Adb @('shell','settings','put','system','font_scale',$oldFontScale) | Out-Null }
        } catch { Write-Warning "Font scale recovery failed: $_"; if (!$failure) { $failure=$_ } }
    }
    if ($restored) { try { Adb @('uninstall','com.perfgames.falley.tests') | Out-Null } catch { if (!$failure) { $failure=$_ } } }
}
if ($failure) { throw $failure }
if ($FirebaseOnly) { Write-Host "PASS: Firebase opt-in, actual SDK initialization, independent collection, private event schema and localized settings. Console delivery remains owner confirmation. Artifacts: $ArtifactRoot" }
elseif ($V43Only) { Write-Host "PASS: focused Falley 4.3 responsive Campaign/avatars/boss and EN/HI/ID measured native layouts. Controlled emulator fixtures; physical motion, audio and performance remain owner checks. Artifacts: $ArtifactRoot" }
elseif ($UxOnly) { Write-Host "PASS: focused Falley 4.3 debug responsive phone/tablet layout, inline Campaign selection, avatar traits, boss emission cues, first-run language/country hints, durable avatar choices, mixed individual ropes, Arcade cards, music lifecycle and EN/HI/ID layout. Controlled fixtures; phone feel and audio audibility remain owner checks. Artifacts: $ArtifactRoot" }
elseif ($ExpansionOnly) { Write-Host "PASS: focused Falley debug Daily Drop/share handoff, mastery/progress migration, wardrobe, six chapter finales and EN/HI/ID expansion UI. Controlled fixtures, not a natural phone playtest. Artifacts: $ArtifactRoot" }
elseif ($PrivacyOnly) { Write-Host "PASS: focused Falley EN/HI/ID policy contact, URL, wrapping/accessibility, blocked browser/mail handoff and reviewed SDK permissions. Artifacts: $ArtifactRoot" }
elseif ($RevisionOnly -or $RevisionSmoke) { Write-Host "PASS: focused Falley campaign locks, burst recovery/budget, drift and visual Help revision checks. Artifacts: $ArtifactRoot" }
elseif ($SplashOnly) { Write-Host "PASS: focused Falley debug splash transition/resource checks; starting-window artwork requires visual review. Artifacts: $ArtifactRoot" }
elseif ($FeatureOnly) { Write-Host "PASS: focused Falley debug localization, paging, jump budget/ceiling and splash checks. Artifacts: $ArtifactRoot" }
elseif ($ProgressionOnly) { Write-Host "PASS: focused Falley debug late steering progression and actual HUD glyph spacing. Artifacts: $ArtifactRoot" }
elseif ($MotionOnly) { Write-Host "PASS: focused Falley debug motion recentering, bilateral tilt/braking and touch handoff. Artifacts: $ArtifactRoot" }
elseif ($StoreOnly) { Write-Host "PASS: Falley V3.2 native 1080x1920 store captures and measured crop metadata. Artifacts: $ArtifactRoot" }
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
