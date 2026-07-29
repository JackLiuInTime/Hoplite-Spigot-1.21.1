<#
Downloads a maven-wrapper.jar into .mvn\wrapper\maven-wrapper.jar
Tries a few known Maven Central locations. Run in project root using PowerShell.
#>

$destDir = Join-Path -Path $PSScriptRoot -ChildPath "..\\.mvn\\wrapper"
$destDir = (Resolve-Path -Path $destDir).ProviderPath
if (!(Test-Path -Path $destDir)) { New-Item -ItemType Directory -Force -Path $destDir | Out-Null }

$dest = Join-Path -Path $destDir -ChildPath "maven-wrapper.jar"

$urls = @(
    'https://repo1.maven.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar',
    'https://repo1.maven.org/maven2/org/apache/maven/wrapper/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar'
)

$success = $false
foreach ($u in $urls) {
    try {
        Write-Host "尝试从 $u 下载..."
        Invoke-WebRequest -Uri $u -OutFile $dest -UseBasicParsing -ErrorAction Stop
        Write-Host "已下载到 $dest"
        $success = $true
        break
    } catch {
        Write-Host "从 $u 下载失败：$($_.Exception.Message)" -ForegroundColor Yellow
    }
}

if (-not $success) {
    Write-Host "下载失败：未能从预设 URL 获取 maven-wrapper.jar。" -ForegroundColor Red
    Write-Host "如果你本机安装了 Maven，请在项目根运行：" -ForegroundColor Cyan
    Write-Host "  mvn -N io.takari:maven:wrapper" -ForegroundColor Cyan
    exit 1
}

exit 0
