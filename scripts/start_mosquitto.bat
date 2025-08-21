@echo off
echo ========================================
echo Starting Mosquitto MQTT Broker
echo ========================================
echo.

REM Check if Mosquitto is installed
where mosquitto >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Mosquitto not found in PATH
    echo Please install Mosquitto first:
    echo 1. Download from https://mosquitto.org/download/
    echo 2. Extract to C:\mosquitto
    echo 3. Add C:\mosquitto to PATH environment variable
    echo.
    echo Or use Chocolatey: choco install mosquitto
    pause
    exit /b 1
)

echo ✅ Mosquitto found
echo.

REM Create data directory if it doesn't exist
if not exist "mosquitto_data" mkdir mosquitto_data

REM Start Mosquitto broker
echo 🚀 Starting Mosquitto broker on port 1883...
echo 📁 Config file: mosquitto_local.conf
echo 📁 Data directory: mosquitto_data
echo 📁 Log file: mosquitto_local.log
echo.
echo Press Ctrl+C to stop the broker
echo.

REM Start the broker with our configuration
mosquitto -c mosquitto_local.conf -v

echo.
echo 🔌 Mosquitto broker stopped
pause
