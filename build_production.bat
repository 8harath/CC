@echo off
echo ========================================
echo Car Crash Detection App - Production Build
echo ========================================
echo.

echo Checking Java installation...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 11 or later and add it to PATH
    pause
    exit /b 1
)

echo Checking Android SDK...
if not exist "%ANDROID_HOME%" (
    echo ERROR: ANDROID_HOME environment variable is not set
    echo Please set ANDROID_HOME to your Android SDK path
    pause
    exit /b 1
)

echo Checking keystore configuration...
if not exist "keystore.properties" (
    echo ERROR: keystore.properties file not found
    echo Please create keystore.properties with your signing configuration
    pause
    exit /b 1
)

echo.
echo Starting production build...
echo.

echo Cleaning previous builds...
call gradlew clean
if %errorlevel% neq 0 (
    echo ERROR: Clean failed
    pause
    exit /b 1
)

echo Building release APK...
call gradlew assembleRelease
if %errorlevel% neq 0 (
    echo ERROR: Build failed
    pause
    exit /b 1
)

echo.
echo Checking for generated APK...
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo.
    echo ========================================
    echo BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo Production APK generated at:
    echo app\build\outputs\apk\release\app-release.apk
    echo.
    echo APK Details:
    dir "app\build\outputs\apk\release\app-release.apk"
    echo.
    echo Ready for distribution!
    echo.
) else (
    echo ERROR: APK not found in expected location
    echo Check build output for errors
    pause
    exit /b 1
)

echo.
echo Build completed successfully!
pause
