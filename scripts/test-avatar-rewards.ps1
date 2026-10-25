param([string]$JdkRoot=$env:JAVA_HOME,[string]$SdkRoot=$env:ANDROID_HOME)
$ErrorActionPreference='Stop'
if(!$JdkRoot -or !$SdkRoot){throw 'Pass JdkRoot and SdkRoot; this JVM storage test uses Android shared-preference interfaces, not an emulator.'}
$root=Split-Path -Parent $PSScriptRoot
$classes=Join-Path $root ('build\avatar-reward-storage-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$androidJar=Join-Path $SdkRoot 'platforms\android-35\android.jar'
$names=@('AvatarRewards','AvatarCatalog','Cosmetics','FalleyProfile','FalleyStrings','MotionSteering','RopeCatalog','StageCatalog','ArcadeRules','DailyDrop','ProfileStorage')
$sources=@($names|ForEach-Object{Join-Path $root ('app\src\main\java\com\perfgames\ropelab\'+$_+'.java')})
$tests=@('tests\AvatarRewardsTest.java','tests\storage\AvatarRewardsStorageTest.java')|ForEach-Object{Join-Path $root $_}
& (Join-Path $JdkRoot 'bin\javac.exe') -encoding UTF-8 -classpath $androidJar -d $classes @sources @tests
if($LASTEXITCODE){throw 'Avatar reward/storage test compilation failed.'}
foreach($name in @('AvatarRewardsTest','AvatarRewardsStorageTest')){
 & (Join-Path $JdkRoot 'bin\java.exe') -cp ($classes+';'+$androidJar) ('com.perfgames.ropelab.'+$name)
 if($LASTEXITCODE){throw ('Avatar reward/storage test failed: '+$name)}
}