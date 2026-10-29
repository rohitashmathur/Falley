param(
    [string]$SdkRoot=$env:ANDROID_HOME,
    [string]$JdkRoot=$env:JAVA_HOME,
    [ValidateSet('com.perfgames.falley.motiontest')][string]$ApplicationId='com.perfgames.falley.motiontest',
    [string]$AppLabel='Falley Motion Test',
    [string]$VersionName='4.4.1-beta-test',
    [ValidateRange(1,2100000000)][int]$VersionCode=18,
    [string]$OutputApk
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if (!$OutputApk) { $OutputApk=Join-Path $root 'build/Falley-debug.apk' }
$OutputApk=[IO.Path]::GetFullPath($OutputApk)
if (!$SdkRoot -or !$JdkRoot) { throw 'Set ANDROID_HOME and JAVA_HOME, or pass SDK36/JDK17 paths.' }
if (!(Test-Path -LiteralPath (Join-Path $SdkRoot 'platforms/android-36/android.jar'))) { throw 'Android SDK platform36 is required.' }
if (!(Test-Path -LiteralPath (Join-Path $root 'app/src/debug/google-services.json'))) { throw 'Missing local debug Firebase config. Run scripts/configure-firebase.ps1 first.' }
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $OutputApk),(Join-Path $root 'build') | Out-Null
$env:JAVA_HOME=$JdkRoot; $env:ANDROID_HOME=$SdkRoot; $env:ANDROID_SDK_ROOT=$SdkRoot
$env:GRADLE_USER_HOME=Join-Path $root 'build/gradle-user-home'
$env:Path=(Join-Path $JdkRoot 'bin')+';'+$env:Path
function Checked([string]$Program,[string[]]$Arguments) {
    & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw ('Debug build step failed: '+[IO.Path]::GetFileName($Program)+' (exit '+$LASTEXITCODE+')') }
}
$key=Join-Path $root 'build/debug.keystore'
if (!(Test-Path -LiteralPath $key)) {
    Checked (Join-Path $JdkRoot 'bin/keytool.exe') @('-genkeypair','-keystore',$key,'-storepass','android','-alias','androiddebugkey','-keypass','android','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Falley Debug,O=Perf Games,C=IN')
}
# Gradle handles Firebase's transitive Android libraries and generated variant resources.
Push-Location $root
try {
    Checked (Join-Path $root 'gradlew.bat') @('--no-daemon','--console=plain',('-PfalleyDebugLabel='+$AppLabel),('-PfalleyDebugVersionName='+$VersionName),('-PfalleyDebugVersionCode='+$VersionCode),':app:assembleDebug')
} finally { Pop-Location }
$source=Join-Path $root 'app/build/outputs/apk/debug/app-debug.apk'
if (!(Test-Path -LiteralPath $source)) { throw 'Gradle completed without the expected debug APK.' }
if ([IO.Path]::GetFullPath($source) -ne $OutputApk) { Copy-Item -LiteralPath $source -Destination $OutputApk }
Checked (Join-Path $SdkRoot 'build-tools/35.0.0/apksigner.bat') @('verify','--verbose',$OutputApk)
Write-Output "Built local Firebase-enabled debug APK: $OutputApk"
