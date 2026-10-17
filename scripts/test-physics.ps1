param([string]$JdkRoot=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
if(!$JdkRoot){throw 'Set JAVA_HOME or pass -JdkRoot (JDK 17).'}
$root=Split-Path -Parent $PSScriptRoot
$classes=Join-Path $root 'build\tests'
New-Item -ItemType Directory -Force $classes | Out-Null
& "$JdkRoot\bin\javac.exe" -encoding UTF-8 -d $classes "$root\app\src\main\java\com\perfgames\ropelab\ArcadeRules.java" "$root\app\src\main\java\com\perfgames\ropelab\StageCatalog.java" "$root\app\src\main\java\com\perfgames\ropelab\Physics.java" "$root\app\src\main\java\com\perfgames\ropelab\TouchSteering.java" "$root\app\src\main\java\com\perfgames\ropelab\MotionSteering.java" "$root\app\src\main\java\com\perfgames\ropelab\FalleyStrings.java" "$root\app\src\main\java\com\perfgames\ropelab\FalleyProfile.java" "$root\app\src\main\java\com\perfgames\ropelab\HazardAudioCue.java" "$root\tests\PhysicsTest.java" "$root\tests\ArcadePhysicsTest.java" "$root\tests\TouchSteeringTest.java" "$root\tests\MotionSteeringTest.java" "$root\tests\MotionGameplayTest.java" "$root\tests\SteeringProgressionTest.java" "$root\tests\FalleyProfileTest.java" "$root\tests\BuzzsawPhysicsTest.java" "$root\tests\HazardAudioCueTest.java" "$root\tests\JumpBudgetTest.java" "$root\tests\CampaignExpansionTest.java" "$root\tests\FalleyStringsTest.java" "$root\tests\StageUnlockTest.java"
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.PhysicsTest
if($LASTEXITCODE -ne 0){throw 'Physics tests failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.ArcadePhysicsTest
if($LASTEXITCODE -ne 0){throw 'Arcade physics tests failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.BuzzsawPhysicsTest
if($LASTEXITCODE -ne 0){throw 'Buzzsaw physics tests failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.HazardAudioCueTest
if($LASTEXITCODE -ne 0){throw 'Hazard audio cue tests failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.TouchSteeringTest
if($LASTEXITCODE -ne 0){throw 'Touch steering tests failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.MotionSteeringTest
if($LASTEXITCODE -ne 0){throw 'Motion steering tests failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.MotionGameplayTest
if($LASTEXITCODE -ne 0){throw 'Motion gameplay tests failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.SteeringProgressionTest
if($LASTEXITCODE -ne 0){throw 'Steering progression tests failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.FalleyProfileTest
if($LASTEXITCODE -ne 0){throw 'Falley profile tests failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.JumpBudgetTest
if($LASTEXITCODE -ne 0){throw 'Jump budget tests failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.CampaignExpansionTest
if($LASTEXITCODE -ne 0){throw 'Campaign expansion tests failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.FalleyStringsTest
if($LASTEXITCODE -ne 0){throw 'Localization tests failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.StageUnlockTest
if($LASTEXITCODE -ne 0){throw 'Stage unlock tests failed'}
