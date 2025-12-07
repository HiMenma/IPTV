@echo off
REM Android APK 打包脚本
REM 只构建 ARM 架构（arm64-v8a 和 armeabi-v7a）用于手机设备

echo ==========================================
echo 开始构建 Android APK
echo ==========================================

REM 清理之前的构建
echo 清理之前的构建...
call flutter clean

REM 获取依赖
echo 获取依赖...
call flutter pub get

REM 构建 APK（只包含 ARM 架构）
echo 构建 APK（ARM 架构）...
call flutter build apk --release --target-platform android-arm,android-arm64

REM 显示构建结果
echo.
echo ==========================================
echo 构建完成！
echo ==========================================
echo.
echo APK 文件位置：
echo   %CD%\build\app\outputs\flutter-apk\app-release.apk
echo.

REM 检查文件是否存在
if exist "build\app\outputs\flutter-apk\app-release.apk" (
    echo 可以使用以下命令安装到手机：
    echo   adb install build\app\outputs\flutter-apk\app-release.apk
) else (
    echo 错误：APK 文件未找到！
)

echo.
echo ==========================================
pause
