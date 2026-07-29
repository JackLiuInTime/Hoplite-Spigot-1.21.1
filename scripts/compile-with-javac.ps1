<#
Usage examples:
  # Provide explicit spigot api jar
  .\scripts\compile-with-javac.ps1 -SpigotJarPath C:\path\to\spigot-api-1.21.1-R0.1-SNAPSHOT.jar

  # Let the script search inside ./lib or ~/.m2
  .\scripts\compile-with-javac.ps1

This script compiles Java sources under `src\main\java` using `javac`, copies
resources from `src\main\resources` into `target\classes`, and creates a
jar at `target\legendary-plugin.jar`.

Requirements:
- JDK (javac, jar) in PATH. 推荐 Java 17.
- spigot-api jar available in `./lib` or `~/.m2/repository`, or pass -SpigotJarPath.
#>

param(
    [string]$SpigotJarPath = "",
    [string]$LibDir = ".\lib",
    [string]$SourceDir = ".\src\main\java",
    [string]$ResourcesDir = ".\src\main\resources",
    [string]$TargetClasses = ".\target\classes",
    [string]$OutJar = ".\target\legendary-plugin.jar",
    [int]$JavaVersion = 17
)

function Find-SpigotJar {
    param([string]$explicit)
    if ($explicit -and (Test-Path $explicit)) { return (Resolve-Path $explicit).ProviderPath }

    if (Test-Path $LibDir) {
        $found = Get-ChildItem -Path $LibDir -Recurse -Filter "*spigot*.jar" -File -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) { return $found.FullName }
    }

    $m2 = Join-Path $env:USERPROFILE ".m2\repository"
    if (Test-Path $m2) {
        $found = Get-ChildItem -Path $m2 -Recurse -Filter "*spigot-api*.jar" -File -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) { return $found.FullName }
    }

    return $null
}

Write-Host "Searching for spigot-api jar..."
$spigot = Find-SpigotJar -explicit $SpigotJarPath
if (-not $spigot) {
    Write-Host "spigot-api jar not found. Place the jar in '$LibDir' or pass -SpigotJarPath. Or run 'mvn -N io.takari:maven:wrapper' locally and use Maven to build." -ForegroundColor Red
    exit 1
}

Write-Host "Using spigot-api: $spigot"

# Collect additional jars from lib dir
$classpathJars = @()
if (Test-Path $LibDir) {
    $classpathJars += Get-ChildItem -Path $LibDir -Filter *.jar -File -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
}

if ($classpathJars -notcontains $spigot) { $classpathJars += $spigot }

$classpath = ($classpathJars -join ";")

# Collect Java sources
$javaFiles = Get-ChildItem -Path $SourceDir -Recurse -Filter *.java -File -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
if (-not $javaFiles) {
    Write-Host "No Java source files found under $SourceDir" -ForegroundColor Red
    exit 1
}

Write-Host "Found $($javaFiles.Count) Java files; preparing to compile..."

# Ensure target/classes exists
New-Item -ItemType Directory -Force -Path $TargetClasses | Out-Null

try {
    $javac = "javac"
    $args = @("--release", "$JavaVersion", "-classpath", $classpath, "-d", $TargetClasses) + $javaFiles
    Write-Host "运行: $javac $($args -join ' ')"
    & $javac @args
    if ($LASTEXITCODE -ne 0) { throw "javac 返回代码 $LASTEXITCODE" }
} catch {
    Write-Host "Compilation failed: $_" -ForegroundColor Red
    exit 1
}

Write-Host "Copying resources (if present)..."
if (Test-Path $ResourcesDir) {
    Copy-Item -Path (Join-Path $ResourcesDir "*") -Destination $TargetClasses -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "Packaging jar to $OutJar"
New-Item -ItemType Directory -Force -Path (Split-Path $OutJar) | Out-Null
try {
    $jar = "jar"
    & $jar cf $OutJar -C $TargetClasses .
    if ($LASTEXITCODE -ne 0) { throw "jar 命令返回代码 $LASTEXITCODE" }
} catch {
    Write-Host "Packaging failed: $_" -ForegroundColor Red
    exit 1
}

Write-Host "Success: created $OutJar" -ForegroundColor Green
exit 0
