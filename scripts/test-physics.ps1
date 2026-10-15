param([string]$JdkRoot=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
if(!$JdkRoot){throw 'Set JAVA_HOME or pass -JdkRoot (JDK 17).'}
$root=Split-Path -Parent $PSScriptRoot
$classes=Join-Path $root 'build\tests'
New-Item -ItemType Directory -Force $classes | Out-Null
& "$JdkRoot\bin\javac.exe" -encoding UTF-8 -d $classes "$root\app\src\main\java\com\perfgames\ropelab\ArcadeRules.java" "$root\app\src\main\java\com\perfgames\ropelab\StageCatalog.java" "$root\app\src\main\java\com\perfgames\ropelab\Physics.java" "$root\app\src\main\java\com\perfgames\ropelab\TouchSteering.java" "$root\app\src\main\java\com\perfgames\ropelab\MotionSteering.java" "$root\app\src\main\java\com\perfgames\ropelab\FalleyProfile.java" "$root\tests\PhysicsTest.java" "$root\tests\ArcadePhysicsTest.java" "$root\tests\TouchSteeringTest.java" "$root\tests\MotionSteeringTest.java" "$root\tests\FalleyProfileTest.java"
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.PhysicsTest
if($LASTEXITCODE -ne 0){throw 'Physics tests failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.ArcadePhysicsTest
if($LASTEXITCODE -ne 0){throw 'Arcade physics tests failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.TouchSteeringTest
if($LASTEXITCODE -ne 0){throw 'Touch steering tests failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.MotionSteeringTest
if($LASTEXITCODE -ne 0){throw 'Motion steering tests failed'}
& "$JdkRoot\bin\java.exe" -cp $classes com.perfgames.ropelab.FalleyProfileTest
if($LASTEXITCODE -ne 0){throw 'Falley profile tests failed'}
