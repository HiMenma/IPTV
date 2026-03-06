@echo off
setlocal enabledelayedexpansion

:: ==========================================
:: IPTV 多平台参数化打包脚本 (Batch)
:: ==========================================

set BUILD_APK=0
set BUILD_MAC=0
set BUILD_WIN=0
set BUILD_LINUX=0
set BUILD_WEB=0
set HAS_ARGS=0

:parse
if "%~1"=="" goto end_parse
set HAS_ARGS=1
if "%~1"=="--all" (
    set BUILD_APK=1& set BUILD_MAC=1& set BUILD_WIN=1& set BUILD_LINUX=1& set BUILD_WEB=1
) else if "%~1"=="--apk" (
    set BUILD_APK=1
) else if "%~1"=="--mac" (
    set BUILD_MAC=1
) else if "%~1"=="--win" (
    set BUILD_WIN=1
) else if "%~1"=="--linux" (
    set BUILD_LINUX=1
) else if "%~1"=="--web" (
    set BUILD_WEB=1
) else if "%~1"=="--help" (
    goto show_help
) else (
    echo [ERROR] 未知参数: %~1
    goto show_help
)
shift
goto parse

:show_help
echo 用法: build_all.bat [参数]
echo.
echo 参数:
echo   --all      构建所有平台
echo   --apk      仅构建 Android APK
echo   --win      仅构建 Windows
echo   --web      仅构建 Web
echo   --help     显示帮助信息
exit /b 0

:end_parse
if %HAS_ARGS%==0 goto show_help

:: 基础设置
set DIST_DIR=dist
if not exist %DIST_DIR% mkdir %DIST_DIR%
for /f "tokens=2 delims=: " %%v in ('findstr "version:" pubspec.yaml') do set APP_VERSION=%%v

echo [INFO] 正在准备构建环境 (Version: %APP_VERSION%)...
call flutter pub get > nul

:: 执行构建
if %BUILD_APK%==1 (
    echo [INFO] 正在构建 Android...
    call flutter build apk --release --target-platform android-arm,android-arm64
    if %errorlevel% equ 0 copy build\app\outputs\flutter-apk\app-release.apk %DIST_DIR%\iptv_player_android_v%APP_VERSION%.apk
)

if %BUILD_WIN%==1 (
    echo [INFO] 正在构建 Windows...
    call flutter build windows --release
    if %errorlevel% equ 0 (
        powershell -Command "Compress-Archive -Path 'build\windows\x64\runner\Release\*' -DestinationPath '%DIST_DIR%\iptv_player_windows_v%APP_VERSION%.zip' -Force"
    )
)

if %BUILD_WEB%==1 (
    echo [INFO] 正在构建 Web...
    call flutter build web --release
    if %errorlevel% equ 0 (
        powershell -Command "Compress-Archive -Path 'build\web\*' -DestinationPath '%DIST_DIR%\iptv_player_web_v%APP_VERSION%.zip' -Force"
    )
)

echo ==========================================
echo 构建结束。请在 %DIST_DIR% 目录下查看产物。
dir %DIST_DIR%
echo ==========================================
pause
