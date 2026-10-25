param(
    [switch]$BuildOnly,
    [switch]$RestoreOnly,
    [string]$SdkRoot=$env:ANDROID_HOME,
    [string]$JdkRoot=$env:JAVA_HOME,
    [string]$SigningDirectory,
    [string]$Device='emulator-5580',
    [ValidateSet('small','tall')][string[]]$Viewport=@('small','tall'),
    [ValidateRange(1,2100000000)][int]$ExpectedVersionCode=14,
    [ValidateRange(0.85,1.6)][double]$FontScale=1.3,
    [string]$ArtifactRoot
)
$ErrorActionPreference='Stop'
if ($BuildOnly -and $RestoreOnly) { throw 'Choose BuildOnly or RestoreOnly.' }
$root=Split-Path -Parent $PSScriptRoot
$frontlineRoot=Split-Path -Parent (Split-Path -Parent $root)
if (!$SdkRoot) { $SdkRoot=Join-Path $frontlineRoot '.toolchain\android-sdk' }
if (!$JdkRoot) { $JdkRoot=(Get-ChildItem -LiteralPath (Join-Path $frontlineRoot '.toolchain') -Directory -Filter 'jdk-*' | Select-Object -First 1).FullName }
if (!$ArtifactRoot) { $ArtifactRoot=Join-Path $root 'build\device\release-smoke' }
$app='com.perfgames.falley'
$testPackage='com.perfgames.falley.releasesmoke'
$component=$testPackage+'/com.perfgames.falley.tests.ReleaseSmokeTest'
$output=Join-Path $root 'build\release-smoke-harness'
$apk=Join-Path $output 'falley-release-smoke-tests.apk'
function Checked([string]$Program,[string[]]$Arguments) { & $Program @Arguments; if ($LASTEXITCODE -ne 0) { throw "Release smoke step failed: $Program ($LASTEXITCODE)" } }
try {
 if (!$RestoreOnly) {
    # Existing upload identity only. This runner never initializes or replaces a key.
    $signing=& (Join-Path $PSScriptRoot 'release-signing.ps1') -JdkRoot $JdkRoot -SigningDirectory $SigningDirectory
    $env:JAVA_HOME=$JdkRoot
    $tools=Join-Path $SdkRoot 'build-tools\35.0.0'
    $androidJar=Join-Path $SdkRoot 'platforms\android-35\android.jar'
    $staging=Join-Path $output ('staging-'+[guid]::NewGuid().ToString('N'))
    $classes=Join-Path $staging 'classes';$dex=Join-Path $staging 'dex'
    New-Item -ItemType Directory -Force -Path $classes,$dex | Out-Null
    $unsigned=Join-Path $staging 'unsigned.apk'
    Checked (Join-Path $tools 'aapt.exe') @('package','-f','-M',(Join-Path $root 'android-tests\AndroidManifest-release-smoke.xml'),'-I',$androidJar,'-F',$unsigned)
    # Framework-only harness: no app implementation on its classpath, no reflection keep rules.
    Checked (Join-Path $JdkRoot 'bin\javac.exe') @('-encoding','UTF-8','-source','8','-target','8','-classpath',$androidJar,'-d',$classes,(Join-Path $root 'android-tests\com\perfgames\falley\tests\ReleaseSmokeTest.java'))
    $jar=Join-Path $staging 'classes.jar'
    Checked (Join-Path $JdkRoot 'bin\jar.exe') @('cf',$jar,'-C',$classes,'.')
    Checked (Join-Path $tools 'd8.bat') @('--lib',$androidJar,'--min-api','24','--output',$dex,$jar)
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive=[IO.Compression.ZipFile]::Open($unsigned,[IO.Compression.ZipArchiveMode]::Update)
    try { [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,(Join-Path $dex 'classes.dex'),'classes.dex') | Out-Null } finally { $archive.Dispose() }
    $aligned=Join-Path $staging 'aligned.apk'
    Checked (Join-Path $tools 'zipalign.exe') @('-f','4',$unsigned,$aligned)
    Checked (Join-Path $tools 'apksigner.bat') @('sign','--ks',$signing.KeyStore,'--ks-key-alias',$signing.Alias,'--ks-pass','env:FALLEY_UPLOAD_STORE_PASSWORD','--key-pass','env:FALLEY_UPLOAD_KEY_PASSWORD','--out',$apk,$aligned)
    Checked (Join-Path $tools 'apksigner.bat') @('verify',$apk)
    if ($BuildOnly) { Write-Host "Built reflection-free release smoke harness: $apk";return }
 }
 $adb=Join-Path $SdkRoot 'platform-tools\adb.exe'
 function Adb([string[]]$Arguments) { $result=& $adb -s $Device @Arguments; if ($LASTEXITCODE -ne 0) { throw "ADB failed on ${Device}: $Arguments" };return $result }
 if ($Device -notmatch '^emulator-\d+$' -or (Adb @('get-state')) -ne 'device' -or (Adb @('shell','getprop','ro.kernel.qemu')) -ne '1') { throw 'Release preference fixtures run only on a disposable emulator, never a physical phone.' }
 if ((Adb @('shell','pm','path',$app)) -notmatch '^package:') { throw 'Install the bundle-derived production APK separately before this smoke test.' }
 New-Item -ItemType Directory -Force -Path $ArtifactRoot | Out-Null
 function Instrument([string]$Mode,[string]$Shot) {
    $result=Adb @('shell','am','instrument','-r','-w','-e','mode',$Mode,'-e','shot',$Shot,'-e','versionCode',([string]$ExpectedVersionCode),$component)
    $result | Write-Output
    $result | Set-Content -LiteralPath (Join-Path $ArtifactRoot "$Shot-$Mode.txt") -Encoding utf8
    $text=$result -join "`n"
    if ($text -notmatch 'INSTRUMENTATION_CODE: -1' -or $text -notmatch 'PASS: Falley release smoke;' -or $text -match 'FAIL:') { throw "Production smoke $Mode failed for $Shot." }
 }
 if ($RestoreOnly) { Instrument 'restore' 'recovery';return }
 Adb @('install','-r','-t',$apk) | Out-Null
 $size=(Adb @('shell','wm','size')) -join "`n";$density=(Adb @('shell','wm','density')) -join "`n"
 $oldSize=if ($size -match 'Override size: (\d+x\d+)') { $Matches[1] } else { 'reset' }
 $oldDensity=if ($density -match 'Override density: (\d+)') { $Matches[1] } else { 'reset' }
 $oldFont=(Adb @('shell','settings','get','system','font_scale')).Trim()
 $failure=$null;$restored=$false
 try {
    foreach ($name in $Viewport) {
        $dimensions=if ($name -eq 'small') { @('480x800','240') } else { @('720x1600','320') }
        Adb @('shell','am','force-stop',$app) | Out-Null
        Adb @('shell','wm','size',$dimensions[0]) | Out-Null
        Adb @('shell','wm','density',$dimensions[1]) | Out-Null
        Adb @('shell','settings','put','system','font_scale',$FontScale.ToString([Globalization.CultureInfo]::InvariantCulture)) | Out-Null
        Start-Sleep -Milliseconds 650
        try { Instrument 'smoke' $name }
        finally { Adb @('pull',"/sdcard/Android/data/$app/files/falley-release-smoke/$name",$ArtifactRoot) | Out-Null }
    }
 } catch { $failure=$_ }
 finally {
    try { Adb @('shell','am','force-stop',$app) | Out-Null;Instrument 'restore' 'recovery';$restored=$true } catch { Write-Warning "Preference recovery failed; retain harness and use -RestoreOnly: $_";if(!$failure){$failure=$_} }
    try { Adb @('shell','wm','size',$oldSize) | Out-Null;Adb @('shell','wm','density',$oldDensity) | Out-Null;if(!$oldFont -or $oldFont -eq 'null'){Adb @('shell','settings','delete','system','font_scale') | Out-Null}else{Adb @('shell','settings','put','system','font_scale',$oldFont) | Out-Null} } catch { if(!$failure){$failure=$_} }
    if($restored){try{Adb @('uninstall',$testPackage) | Out-Null}catch{if(!$failure){$failure=$_}}}
 }
 if($failure){throw $failure}
 Write-Host "PASS: reflection-free Falley production smoke and verified preference/display recovery. Artifacts: $ArtifactRoot"
} finally {
 $env:FALLEY_UPLOAD_KEYSTORE=$null;$env:FALLEY_UPLOAD_STORE_PASSWORD=$null;$env:FALLEY_UPLOAD_KEY_ALIAS=$null;$env:FALLEY_UPLOAD_KEY_PASSWORD=$null
}