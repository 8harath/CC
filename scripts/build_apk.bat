@echo off
echo Building Car Crash Detection APK...
echo.

REM Check if gradlew.bat exists
if not exist "gradlew.bat" (
    echo Error: gradlew.bat not found!
    echo Please make sure you're in the project root directory.
    pause
    exit /b 1
)

REM Clean previous builds
echo Cleaning previous builds...
call gradlew.bat clean

REM Build debug APK
echo Building debug APK...
call gradlew.bat assembleDebug

REM Check if build was successful
if %ERRORLEVEL% EQU 0 (
    echo.
    echo Build successful! APK location:
    echo app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo You can now install this APK on your Android device.
    echo.
) else (
    echo.
    echo Build failed! Please check the error messages above.
    echo.
)

pause 