@echo off
echo Building Production APK for Car Crash Detection App...
echo.

REM Clean previous builds
echo Cleaning previous builds...
call gradlew.bat clean
if %ERRORLEVEL% neq 0 (
    echo Error: Failed to clean project
    pause
    exit /b 1
)

REM Build debug version (since we don't have keystore)
echo Building debug APK...
call gradlew.bat assembleDebug
if %ERRORLEVEL% neq 0 (
    echo Error: Failed to build debug APK
    pause
    exit /b 1
)

echo.
echo Build completed successfully!
echo APK location: app\build\outputs\apk\debug\app-debug.apk
echo.
echo Note: This is a debug build. For production release, you need to:
echo 1. Create a keystore file
echo 2. Configure keystore.properties
echo 3. Run: gradlew.bat assembleRelease
echo.
pause
