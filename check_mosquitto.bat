@echo off
echo Checking Mosquitto MQTT Broker Setup...
echo.

REM Check if Mosquitto service is running
echo 1. Checking Mosquitto service status...
sc query mosquitto > nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    ✓ Mosquitto service found
    sc query mosquitto | findstr "STATE"
) else (
    echo    ✗ Mosquitto service not found
    echo    Please ensure Mosquitto is installed and running
)

echo.

REM Check if port 1883 is listening
echo 2. Checking if port 1883 is listening...
netstat -an | findstr ":1883" > nul
if %ERRORLEVEL% EQU 0 (
    echo    ✓ Port 1883 is listening
    netstat -an | findstr ":1883"
) else (
    echo    ✗ Port 1883 is not listening
    echo    Mosquitto may not be running or configured properly
)

echo.

REM Get local IP address
echo 3. Your local IP addresses:
ipconfig | findstr "IPv4"
echo.
echo    Use one of these IPs to connect from Android devices
echo    (usually starts with 192.168.x.x)

echo.

REM Test MQTT connection if mosquitto_pub is available
echo 4. Testing MQTT connection...
where mosquitto_pub > nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    ✓ mosquitto_pub found, testing connection...
    mosquitto_pub -h localhost -t test/connection -m "Android test message" -q 1
    if %ERRORLEVEL% EQU 0 (
        echo    ✓ MQTT connection test successful
    ) else (
        echo    ✗ MQTT connection test failed
    )
) else (
    echo    ⚠ mosquitto_pub not found in PATH
    echo    You can still use the broker, but testing tools are not available
)

echo.
echo ========================================
echo Mosquitto Setup Summary:
echo.
echo For Android app connection, use:
echo Broker URL: tcp://YOUR_IP_ADDRESS:1883
echo Port: 1883
echo Username: (leave empty if not configured)
echo Password: (leave empty if not configured)
echo.
echo Topics to use:
echo - emergency/alerts (for emergency broadcasts)
echo - emergency/status (for status updates)  
echo - emergency/response (for responder acknowledgments)
echo ========================================

pause 