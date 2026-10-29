param(
    [string]$SdkRoot=$env:ANDROID_HOME,
    [string]$JdkRoot=$env:JAVA_HOME,
    [ValidatePattern('^emulator-\d+$')][string]$Device='emulator-5580',
    [Parameter(Mandatory=$true)][string]$ArtifactRoot
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if (!$SdkRoot -or !$JdkRoot) { throw 'Pass Android SDK and JDK17 paths.' }
$adb=Join-Path $SdkRoot 'platform-tools/adb.exe'
$app='com.perfgames.falley.motiontest'
$component='com.perfgames.falley.tests/com.perfgames.falley.tests.NativeFalleyTest'
function Adb([string[]]$Arguments) { $result=& $adb -s $Device @Arguments;if ($LASTEXITCODE -ne 0) {throw ('ADB verification step failed on '+$Device)};return $result }
if ((Adb @('shell','getprop','ro.kernel.qemu')).Trim() -ne '1') { throw 'An intentional debug crash may run only on an owned emulator.' }
if ((Adb @('shell','pm','path',$app)) -notmatch '^package:') { throw 'Install the current Motion Test debug APK first.' }
New-Item -ItemType Directory -Force -Path $ArtifactRoot | Out-Null
& (Join-Path $PSScriptRoot 'test-ui.ps1') -BuildOnly -FirebaseOnly -SdkRoot $SdkRoot -JdkRoot $JdkRoot -Device $Device -AppPackage $app
if ($LASTEXITCODE -ne 0) { throw 'Unable to compile the debug-only crash fixture.' }
$harness=Join-Path $root ('build/android-ui-falley/'+$Device+'/falley-ui-tests.apk')
Adb @('install','-r','-t',$harness) | Out-Null
$oldLogging=(Adb @('shell','getprop','log.tag.FirebaseCrashlytics')).Trim()
$failure=$null;$restored=$false
try {
    Adb @('shell','am','force-stop',$app) | Out-Null
    Adb @('shell','setprop','log.tag.FirebaseCrashlytics','DEBUG') | Out-Null
    # This is a disposable test emulator. Keep a clean bounded log for this synthetic crash.
    Adb @('logcat','-c') | Out-Null
    $result=Adb @('shell','am','instrument','-r','-w','-e','mode','firebasecrash','-e','shot','intentional-crash',$component)
    $result | Set-Content -LiteralPath (Join-Path $ArtifactRoot 'intentional-crash.txt') -Encoding utf8
    if (($result -join "`n") -notmatch 'Process crashed') { throw 'The explicit debug crash did not terminate the target process as expected.' }
    # Do not run a preference-clearing fixture before reopening: the SDK must see the opted-in crash state.
    Adb @('shell','am','start','-W','-n',($app+'/com.perfgames.ropelab.MainActivity')) | Out-Null
    $logs='';$enqueued=$false;$boundSession=$false
    for ($attempt=0;$attempt -lt 40;$attempt++) {
        Start-Sleep -Milliseconds 750
        $logs=(Adb @('logcat','-d','-t','6000')) -join "`n"
        $fatalAt=$logs.IndexOf('Falley debug Crashlytics verification')
        if ($fatalAt -ge 0) {
            $opened=[regex]::Matches($logs.Substring(0,$fatalAt),'Opening a new session with ID ([A-Za-z0-9-]+)')
            if ($opened.Count -gt 0) {
                $syntheticSession=$opened[$opened.Count-1].Groups[1].Value
                $enqueued=$logs -match ('Crashlytics report successfully enqueued to DataTransport: '+[regex]::Escape($syntheticSession)+'(?:\s|$)')
                $boundSession=$enqueued
            }
        }
        if ($enqueued) { break }
    }
    $logs | Set-Content -LiteralPath (Join-Path $ArtifactRoot 'sdk-logcat-private.txt') -Encoding utf8
    Adb @('pull',"/sdcard/Android/data/$app/files/falley-native-firebase/intentional-crash",$ArtifactRoot) | Out-Null
    $fatal=$logs -match 'Falley debug Crashlytics verification'
    $initialized=$logs -match 'Initializing Firebase Crashlytics'
    $receipt=[ordered]@{package=$app;debugOnly=$true;expectedProcessCrash=$true;syntheticExceptionObserved=$fatal;sdkInitialized=$initialized;reportEnqueuedBySdk=$enqueued;syntheticSessionBound=$boundSession;firebaseConsoleConfirmed=$false;note='SDK transport acceptance is not Firebase Console dashboard confirmation. Raw logs remain local and must be reviewed/redacted before sharing.'}
    $receipt | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $ArtifactRoot 'crash-verification.json') -Encoding utf8
    if (!$fatal -or !$initialized -or !$enqueued) { throw 'The synthetic crash occurred, but SDK report enqueue was not confirmed in the bounded check. Inspect private logs and Firebase Console.' }
    Write-Output 'PASS: intentional debug crash captured and queued by Crashlytics after reopening; dashboard arrival still needs console confirmation.'
} catch { $failure=$_ }
finally {
    try {
        Adb @('shell','am','force-stop',$app) | Out-Null
        $recovery=Adb @('shell','am','instrument','-r','-w','-e','mode','restore','-e','shot','recovery',$component)
        $recovery | Set-Content -LiteralPath (Join-Path $ArtifactRoot 'recovery-restore.txt') -Encoding utf8
        if (($recovery -join "`n") -notmatch 'PASS: native Falley restore;') { throw 'Original preferences were not confirmed restored; retain the harness and recover with test-ui.ps1 -RestoreOnly.' }
        $restored=$true
        Adb @('shell','am','force-stop',$app) | Out-Null
    } catch { if (!$failure) {$failure=$_} }
    try { Adb @('shell','setprop','log.tag.FirebaseCrashlytics',$(if ($oldLogging) {$oldLogging} else {"''"})) | Out-Null } catch { if (!$failure) {$failure=$_} }
    if ($restored) {try{Adb @('uninstall','com.perfgames.falley.tests') | Out-Null}catch{if(!$failure){$failure=$_}}}
}
if ($failure) {throw $failure}
