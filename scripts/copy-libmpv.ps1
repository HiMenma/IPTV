$ErrorActionPreference = "Stop"

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "libmpv 库文件复制脚本 (Windows)" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$ResourcesDir = "composeApp/src/desktopMain/resources/native/windows-x86_64"

Write-Host "平台: Windows x86_64" -ForegroundColor Yellow
Write-Host "目标目录: $ResourcesDir" -ForegroundColor Yellow
Write-Host ""

Write-Host "请确保已下载 MPV Windows 构建版本" -ForegroundColor White
Write-Host "下载地址: https://github.com/shinchiro/mpv-winbuild-cmake/releases" -ForegroundColor Cyan
Write-Host ""

$MpvDir = Read-Host "请输入 MPV 解压目录路径"

if (-not (Test-Path $MpvDir)) {
    Write-Host ""
    Write-Host "❌ 错误: 目录不存在: $MpvDir" -ForegroundColor Red
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "创建目标目录..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path $ResourcesDir | Out-Null

# 查找 libmpv DLL
$LibmpvPath = $null
$LibmpvName = $null

if (Test-Path "$MpvDir/libmpv-2.dll") {
    $LibmpvPath = "$MpvDir/libmpv-2.dll"
    $LibmpvName = "libmpv-2.dll"
} elseif (Test-Path "$MpvDir/mpv-2.dll") {
    $LibmpvPath = "$MpvDir/mpv-2.dll"
    $LibmpvName = "mpv-2.dll"
} else {
    # 尝试在子目录中查找
    $Found = Get-ChildItem -Path $MpvDir -Recurse -Filter "libmpv-2.dll" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($Found) {
        $LibmpvPath = $Found.FullName
        $LibmpvName = "libmpv-2.dll"
    } else {
        $Found = Get-ChildItem -Path $MpvDir -Recurse -Filter "mpv-2.dll" -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($Found) {
            $LibmpvPath = $Found.FullName
            $LibmpvName = "mpv-2.dll"
        }
    }
}

if (-not $LibmpvPath) {
    Write-Host ""
    Write-Host "❌ 错误: 在指定目录中找不到 libmpv-2.dll 或 mpv-2.dll" -ForegroundColor Red
    Write-Host ""
    Write-Host "请确保:" -ForegroundColor Yellow
    Write-Host "  1. 已下载正确的 MPV Windows 构建版本" -ForegroundColor White
    Write-Host "  2. 已解压到指定目录" -ForegroundColor White
    Write-Host "  3. 目录中包含 libmpv-2.dll 或 mpv-2.dll" -ForegroundColor White
    Write-Host ""
    exit 1
}

Write-Host "找到库文件: $LibmpvPath" -ForegroundColor Green
Write-Host ""

# 复制库文件
Write-Host "复制 $LibmpvName..." -ForegroundColor Yellow
$TargetPath = "$ResourcesDir/libmpv-2.dll"
Copy-Item $LibmpvPath $TargetPath -Force

# 验证复制
if (Test-Path $TargetPath) {
    Write-Host "✅ 库文件复制成功" -ForegroundColor Green
    Write-Host ""
    
    # 显示文件信息
    Write-Host "文件信息:" -ForegroundColor Yellow
    $FileInfo = Get-Item $TargetPath
    Write-Host "  路径: $($FileInfo.FullName)" -ForegroundColor White
    Write-Host "  大小: $([math]::Round($FileInfo.Length / 1MB, 2)) MB" -ForegroundColor White
    Write-Host "  修改时间: $($FileInfo.LastWriteTime)" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "❌ 复制失败" -ForegroundColor Red
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "✅ 库文件已成功复制到项目中" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "下一步:" -ForegroundColor Yellow
Write-Host "  1. 编译项目: .\gradlew.bat :composeApp:desktopJar" -ForegroundColor White
Write-Host "  2. 运行应用: .\gradlew.bat :composeApp:run" -ForegroundColor White
Write-Host "  3. 打包应用: .\gradlew.bat :composeApp:packageDistributionForCurrentOS" -ForegroundColor White
Write-Host ""
