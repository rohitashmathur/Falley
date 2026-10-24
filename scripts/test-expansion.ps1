param([string]$JdkRoot=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
if(!$JdkRoot){throw 'Pass JdkRoot for JDK17.'}
$root=Split-Path -Parent $PSScriptRoot
$classes=Join-Path $root 'build\expansion-tests'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$portable=@('ArcadeRules','StageCatalog','Physics','TouchSteering','MotionSteering','FalleyStrings','ExpansionStrings','FalleyProfile','HazardAudioCue','DailyDrop','Cosmetics','AvatarCatalog','ChapterStories','UxRules','UxStrings')
$sources=@($portable | ForEach-Object { Join-Path $root ('app\src\main\java\com\perfgames\ropelab\'+$_+'.java') })
$tests=@(Get-ChildItem -LiteralPath (Join-Path $root 'tests') -File -Filter '*Test.java' | Sort-Object Name)
& (Join-Path $JdkRoot 'bin\javac.exe') -encoding UTF-8 -d $classes @sources @($tests.FullName)
if($LASTEXITCODE -ne 0){throw 'Portable test compilation failed.'}
foreach($test in $tests){
    & (Join-Path $JdkRoot 'bin\java.exe') -cp $classes ('com.perfgames.ropelab.'+$test.BaseName)
    if($LASTEXITCODE -ne 0){throw ('Portable test failed: '+$test.BaseName)}
}
