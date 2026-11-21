$ErrorActionPreference = 'Stop'

Write-Host "Setting up classpath..."
$baseJars = @(
    "C:\Users\xeoniii\AppData\Roaming\.sklauncher\versions\26.3\26.3.jar",
    "C:\Users\xeoniii\AppData\Roaming\.sklauncher\libraries\net\fabricmc\fabric-loader\0.20.1\fabric-loader-0.20.1.jar",
    "C:\Users\xeoniii\AppData\Roaming\.sklauncher\libraries\org\slf4j\slf4j-api\2.0.17\slf4j-api-2.0.17.jar",
    "C:\Users\xeoniii\AppData\Roaming\.sklauncher\libraries\com\google\code\gson\gson\2.14.0\gson-2.14.0.jar",
    "C:\Users\xeoniii\AppData\Roaming\.sklauncher\libraries\com\mojang\brigadier\1.3.11\brigadier-1.3.11.jar",
    "C:\Users\xeoniii\AppData\Roaming\.sklauncher\libraries\org\lwjgl\lwjgl-sdl\3.4.3\lwjgl-sdl-3.4.3.jar",
    (Resolve-Path "extracted").Path
)

$fabricSubJars = (Get-ChildItem -Path "fabric_libs\META-INF\jars\*.jar").FullName
$classpath = ($baseJars + $fabricSubJars) -join ";"

if (Test-Path "build") {
    Remove-Item -Recurse -Force "build"
}
New-Item -ItemType Directory -Force -Path "build\classes" | Out-Null
New-Item -ItemType Directory -Force -Path "build\jar-staging\META-INF" | Out-Null

$sources = (Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java").FullName
Write-Host "Compiling $($sources.Count) Java sources..."
& javac -encoding UTF-8 -cp $classpath -d "build\classes" $sources

Write-Host "Compilation succeeded!"

Write-Host "Staging JAR contents..."
# 1. Copy compiled mod classes
Copy-Item -Path "build\classes\*" -Destination "build\jar-staging" -Recurse

# 2. Copy resources
Copy-Item -Path "src\main\resources\*" -Destination "build\jar-staging" -Recurse

# 3. Copy bundled Java-WebSocket classes and metadata from extracted
Copy-Item -Path "extracted\org" -Destination "build\jar-staging\org" -Recurse
if (Test-Path "extracted\META-INF\maven") {
    Copy-Item -Path "extracted\META-INF\maven" -Destination "build\jar-staging\META-INF\maven" -Recurse
}
if (Test-Path "extracted\META-INF\versions") {
    Copy-Item -Path "extracted\META-INF\versions" -Destination "build\jar-staging\META-INF\versions" -Recurse
}

# 4. Create MANIFEST.MF
$manifestContent = @"
Manifest-Version: 1.0
Fabric-Loom-Client-Only-Entries: mewsicbridge/client/MewsicBridgeClient.class;mewsicbridge/client/MewsicHudOverlay.class;assets/mewsic-bridge/lang/en_us.json
Fabric-Loom-Split-Environment: true
Fabric-Mapping-Namespace: official
Fabric-Gradle-Version: 9.6.0
Fabric-Loom-Version: 1.17.0
Fabric-Minecraft-Version: 26.3
Fabric-Loader-Version: 0.20.1

"@
Set-Content -Path "build\jar-staging\META-INF\MANIFEST.MF" -Value $manifestContent -Encoding ASCII

# 5. Build final JAR
$jarName = "mewsic-bridge-v2.0+26.3.jar"
$targetJar = Join-Path (Get-Location).Path $jarName
if (Test-Path $targetJar) {
    Remove-Item -Force $targetJar
}

Write-Host "Creating JAR $jarName..."
& jar -cfm $targetJar "build\jar-staging\META-INF\MANIFEST.MF" -C "build\jar-staging" .

$altJar = Join-Path (Get-Location).Path "mewsic-bridge-2.0+26.3.jar"
Copy-Item $targetJar $altJar -Force

Write-Host "Built $targetJar ($( (Get-Item $targetJar).Length ) bytes) successfully!"

# 6. Deploy to launcher instances and clean up older versions
$instances = @(
    "C:\Users\xeoniii\AppData\Roaming\.sklauncher\instances\fo-26-3-another-one\mods",
    "C:\Users\xeoniii\AppData\Roaming\.sklauncher\instances\fo-26-3\mods"
)

foreach ($dir in $instances) {
    if (Test-Path $dir) {
        $oldJars = Get-ChildItem -Path $dir -Filter "mewsic-bridge-*.jar" | Where-Object { $_.Name -ne $jarName }
        foreach ($old in $oldJars) {
            try {
                Remove-Item $old.FullName -Force -ErrorAction Stop
                Write-Host "Removed older version $($old.Name)"
            } catch {
                Write-Host "File $($old.Name) locked by running game process; overwriting with new contents."
                Copy-Item $targetJar $old.FullName -Force
            }
        }
        Copy-Item $targetJar (Join-Path $dir $jarName) -Force
        Write-Host "Deployed to $dir\$jarName"
    }
}
