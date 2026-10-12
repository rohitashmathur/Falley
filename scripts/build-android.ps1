param([string]$SdkRoot=$env:ANDROID_HOME,[string]$JdkRoot=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if(!$SdkRoot -or !$JdkRoot){throw 'Set ANDROID_HOME and JAVA_HOME, or pass -SdkRoot and -JdkRoot. Requires SDK 35 / build tools 35.0.0 and JDK 17.'}
$tools=Join-Path $SdkRoot 'build-tools\35.0.0'
$androidJar=Join-Path $SdkRoot 'platforms\android-35\android.jar'
$env:JAVA_HOME=$JdkRoot
$env:Path=(Join-Path $JdkRoot 'bin')+';'+$env:Path
$build=Join-Path $root 'build'
New-Item -ItemType Directory -Force "$build\generated","$build\classes","$build\dex" | Out-Null
function Run([string]$exe,[string[]]$argsList){ & $exe @argsList; if($LASTEXITCODE -ne 0){throw "$exe failed ($LASTEXITCODE)"} }
$manifest=(Get-Content "$root\app\src\main\AndroidManifest.xml" -Raw).Replace('<manifest ', '<manifest package="com.perfgames.ropelab" ')
$manifest | Set-Content -Encoding utf8 "$build\AndroidManifest.xml"
Run "$tools\aapt.exe" @('package','-f','-m','--debug-mode','-J',"$build\generated",'-M',"$build\AndroidManifest.xml",'-S',"$root\app\src\main\res",'-I',$androidJar,'-F',"$build\unsigned.apk",'--version-code','2','--version-name','0.2.0')
$files=@((Get-ChildItem "$root\app\src\main\java","$build\generated" -Recurse -Filter '*.java').FullName)
Run 'javac.exe' (@('-encoding','UTF-8','-source','8','-target','8','-classpath',$androidJar,'-d',"$build\classes")+$files)
Run 'jar.exe' @('cf',"$build\classes.jar",'-C',"$build\classes",'.')
Run "$tools\d8.bat" @('--lib',$androidJar,'--min-api','24','--output',"$build\dex","$build\classes.jar")
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::Open("$build\unsigned.apk",[IO.Compression.ZipArchiveMode]::Update)
try{[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,"$build\dex\classes.dex",'classes.dex') | Out-Null}finally{$zip.Dispose()}
Run "$tools\zipalign.exe" @('-f','-p','4',"$build\unsigned.apk","$build\aligned.apk")
$key="$build\debug.keystore"
if(!(Test-Path $key)){Run 'keytool.exe' @('-genkeypair','-keystore',$key,'-storepass','android','-alias','androiddebugkey','-keypass','android','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Rope Lab Debug,O=Perf Games,C=IN')}
Run "$tools\apksigner.bat" @('sign','--ks',$key,'--ks-pass','pass:android','--key-pass','pass:android','--out',"$root\RopeLab-debug.apk","$build\aligned.apk")
Run "$tools\apksigner.bat" @('verify','--verbose',"$root\RopeLab-debug.apk")
Write-Host "Built $root\RopeLab-debug.apk"
