@echo off
echo ========================================
echo Local MQTT Setup for Car Crash Detection
echo ========================================
echo.

echo Checking Mosquitto installation...
where mosquitto >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Mosquitto not found. Please install Mosquitto first.
    echo Download from: https://mosquitto.org/download/
    pause
    exit /b 1
)

echo Mosquitto found. Checking if service is running...
netstat -an | findstr :1883 >nul
if %errorlevel% equ 0 (
    echo ✅ Mosquitto is running on port 1883
) else (
    echo ⚠️  Mosquitto not running on port 1883
    echo Starting Mosquitto service...
    net start mosquitto
    if %errorlevel% equ 0 (
        echo ✅ Mosquitto service started successfully
    ) else (
        echo ❌ Failed to start Mosquitto service
        echo Please start it manually: net start mosquitto
    )
)

echo.
echo Getting your IP address...
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /C:"IPv4 Address"') do (
    set IP=%%a
    goto :found_ip
)
:found_ip
set IP=%IP: =%
echo Your IP address: %IP%

echo.
echo Testing MQTT connectivity...
echo Publishing test message...
mosquitto_pub -h localhost -t "test/connection" -m "Hello from setup script!" -q 1
if %errorlevel% equ 0 (
    echo ✅ MQTT publish test successful
) else (
    echo ❌ MQTT publish test failed
)

echo.
echo ========================================
echo Setup Summary:
echo ========================================
echo Broker IP: %IP%
echo Broker Port: 1883
echo Status: Mosquitto running
echo.
echo Next steps:
echo 1. Configure Smartphone A (Publisher) with IP: %IP%
echo 2. Configure Smartphone B (Subscriber) with IP: %IP%
echo 3. Test communication between phones
echo.
echo For detailed instructions, see LOCAL_MQTT_SETUP_GUIDE.md
echo ========================================
pause
