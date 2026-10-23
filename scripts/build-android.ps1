param(
    [string]$SdkRoot=$env:ANDROID_HOME,
    [string]$JdkRoot=$env:JAVA_HOME,
    [ValidatePattern('^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$')][string]$ApplicationId='com.perfgames.falley',
    [string]$AppLabel='Falley',
    [string]$VersionName='3.3.1',
    [ValidateRange(1,2100000000)][int]$VersionCode=11,
    [string]$OutputApk
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if (!$OutputApk) { $OutputApk=Join-Path $root 'Falley-debug.apk' }
$OutputApk=[IO.Path]::GetFullPath($OutputApk)
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $OutputApk) | Out-Null
if(!$SdkRoot -or !$JdkRoot){throw 'Set ANDROID_HOME and JAVA_HOME, or pass -SdkRoot and -JdkRoot. Requires SDK 36 / build tools 35.0.0 and JDK 17.'}
$tools=Join-Path $SdkRoot 'build-tools\35.0.0'
$androidJar=Join-Path $SdkRoot 'platforms\android-36\android.jar'
if(!(Test-Path -LiteralPath $androidJar)){throw 'Android SDK platform 36 is missing. Install platforms;android-36 before building Falley.'}
$env:JAVA_HOME=$JdkRoot
$env:Path=(Join-Path $JdkRoot 'bin')+';'+$env:Path
$build=Join-Path $root 'build'
# Fresh staging avoids carrying obsolete classes/resources into a rebuilt APK.
# The signing key stays outside staging so its identity is preserved across builds.
$stage=Join-Path $build ('falley-'+[guid]::NewGuid().ToString('N').Substring(0,8))
New-Item -ItemType Directory -Force "$stage\generated","$stage\classes","$stage\dex" | Out-Null
function Run([string]$exe,[string[]]$argsList){ & $exe @argsList; if($LASTEXITCODE -ne 0){throw "$exe failed ($LASTEXITCODE)"} }
$manifest=(Get-Content "$root\app\src\main\AndroidManifest.xml" -Raw).Replace('<manifest ', ('<manifest package="'+$ApplicationId+'" '))
$manifest=$manifest.Replace('android:label="Falley"',('android:label="'+[Security.SecurityElement]::Escape($AppLabel)+'"'))
$manifest | Set-Content -Encoding utf8 "$stage\AndroidManifest.xml"
Run "$tools\aapt.exe" @('package','-f','-m','--debug-mode','--custom-package','com.perfgames.ropelab','-J',"$stage\generated",'-M',"$stage\AndroidManifest.xml",'-S',"$root\app\src\main\res",'-I',$androidJar,'-F',"$stage\unsigned.apk",'--version-code',([string]$VersionCode),'--version-name',$VersionName)
$files=@((Get-ChildItem "$root\app\src\main\java","$stage\generated" -Recurse -Filter '*.java').FullName)
Run 'javac.exe' (@('-encoding','UTF-8','-source','8','-target','8','-classpath',$androidJar,'-d',"$stage\classes")+$files)
Run 'jar.exe' @('cf',"$stage\classes.jar",'-C',"$stage\classes",'.')
Run "$tools\d8.bat" @('--lib',$androidJar,'--min-api','24','--output',"$stage\dex","$stage\classes.jar")
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::Open("$stage\unsigned.apk",[IO.Compression.ZipArchiveMode]::Update)
try{[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,"$stage\dex\classes.dex",'classes.dex') | Out-Null}finally{$zip.Dispose()}
Run "$tools\zipalign.exe" @('-f','-p','4',"$stage\unsigned.apk","$stage\aligned.apk")
$key="$build\debug.keystore"
if(!(Test-Path $key)){Run 'keytool.exe' @('-genkeypair','-keystore',$key,'-storepass','android','-alias','androiddebugkey','-keypass','android','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Falley Debug,O=Perf Games,C=IN')}
Run "$tools\apksigner.bat" @('sign','--ks',$key,'--ks-pass','pass:android','--key-pass','pass:android','--out',$OutputApk,"$stage\aligned.apk")
Run "$tools\apksigner.bat" @('verify','--verbose',$OutputApk)
Write-Host "Built $OutputApk"
