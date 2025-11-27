@echo off
REM 桌面平台重新打包脚本 (Windows)
REM 使用方法: repackage.bat [msi|exe]

echo ================================
echo IPTV Player 桌面平台重新打包
echo ================================
echo.

REM 清理旧构建
echo 🧹 清理旧构建...
call gradlew.bat clean
if errorlevel 1 (
    echo ❌ 清理失败
    exit /b 1
)
echo ✅ 清理完成
echo.

REM 根据参数选择打包类型
set PACKAGE_TYPE=%1
if "%PACKAGE_TYPE%"=="" set PACKAGE_TYPE=msi

if "%PACKAGE_TYPE%"=="msi" (
    echo 📦 开始打包 Windows MSI...
    call gradlew.bat packageMsi
    if errorlevel 1 (
        echo ❌ MSI 打包失败
        exit /b 1
    )
    echo ✅ MSI 打包完成
    echo 📍 文件位置: composeApp\build\compose\binaries\main\msi\
) else if "%PACKAGE_TYPE%"=="exe" (
    echo 📦 开始打包 Windows EXE...
    call gradlew.bat packageExe
    if errorlevel 1 (
        echo ❌ EXE 打包失败
        exit /b 1
    )
    echo ✅ EXE 打包完成
    echo 📍 文件位置: composeApp\build\compose\binaries\main\exe\
) else (
    echo ❌ 未知的打包类型: %PACKAGE_TYPE%
    echo 使用方法: repackage.bat [msi^|exe]
    exit /b 1
)

echo.
echo ================================
echo ✅ 打包完成！
echo ================================
echo.
echo 修复内容：
echo   ✅ 添加了 java.sql 模块支持
echo   ✅ 修正了图标文件路径
echo   ✅ 启用了完整运行时模块
echo.
echo 请安装并测试新打包的应用：
echo   1. 检查图标是否正确显示
echo   2. 确认没有 DriverManager 错误
echo   3. 测试添加播放列表功能
echo.
pause
