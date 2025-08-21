@echo off
echo ========================================
echo MQTT Communication Test Suite
echo ========================================
echo.

REM Check if Python is available
python --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Python is not installed or not in PATH
    echo Please install Python from https://python.org
    pause
    exit /b 1
)

REM Check if paho-mqtt is installed
python -c "import paho.mqtt.client" >nul 2>&1
if errorlevel 1 (
    echo ⚠️  paho-mqtt not installed. Installing...
    pip install paho-mqtt
    if errorlevel 1 (
        echo ❌ Failed to install paho-mqtt
        pause
        exit /b 1
    )
)

echo ✅ Python and paho-mqtt are available
echo.

REM Get broker IP from user
set /p BROKER_IP="Enter your broker IP address (e.g., 192.168.1.100): "
if "%BROKER_IP%"=="" (
    echo ❌ No IP address provided
    pause
    exit /b 1
)

echo.
echo ========================================
echo Testing MQTT Communication
echo Broker: %BROKER_IP%:1883
echo ========================================
echo.

REM Run the simple test first
echo 🚀 Running Simple MQTT Test...
python scripts\test_mqtt_simple.py %BROKER_IP% 1883
echo.

REM Run the comprehensive diagnostic
echo 🔍 Running Comprehensive Diagnostic...
python scripts\diagnose_mqtt_communication.py %BROKER_IP% 1883
echo.

echo ========================================
echo Test Complete
echo ========================================
echo.
echo 📋 Next Steps:
echo 1. If tests pass: Your MQTT setup is working
echo 2. If tests fail: Check your Mosquitto broker
echo 3. Test with Android apps using the same IP
echo.
echo 💡 Tips:
echo - Ensure Mosquitto is running: mosquitto -p 1883
echo - Check firewall allows port 1883
echo - Verify both phones are on same network
echo.
pause
