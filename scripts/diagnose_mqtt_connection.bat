@echo off
echo ========================================
echo MQTT Connection Diagnostic Tool
echo ========================================
echo.

echo Step 1: Checking Mosquitto status...
netstat -an | findstr :1883
if %errorlevel% equ 0 (
    echo ✅ Mosquitto is running on port 1883
) else (
    echo ❌ Mosquitto not running on port 1883
    echo Starting Mosquitto...
    net start mosquitto
)

echo.
echo Step 2: Getting your IP address...
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /C:"IPv4 Address"') do (
    set IP=%%a
    goto :found_ip
)
:found_ip
set IP=%IP: =%
echo Your IP address: %IP%

echo.
echo Step 3: Testing local MQTT connectivity...
mosquitto_pub -h localhost -t "test/local" -m "Local test message" -q 1
if %errorlevel% equ 0 (
    echo ✅ Local MQTT connection successful
) else (
    echo ❌ Local MQTT connection failed
)

echo.
echo Step 4: Testing external MQTT connectivity...
mosquitto_pub -h %IP% -t "test/external" -m "External test message" -q 1
if %errorlevel% equ 0 (
    echo ✅ External MQTT connection successful
) else (
    echo ❌ External MQTT connection failed
)

echo.
echo Step 5: Checking firewall rules...
netsh advfirewall firewall show rule name="MQTT" >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ MQTT firewall rule exists
) else (
    echo ⚠️  MQTT firewall rule not found
    echo Adding MQTT firewall rule...
    netsh advfirewall firewall add rule name="MQTT" dir=in action=allow protocol=TCP localport=1883
)

echo.
echo ========================================
echo Diagnostic Summary:
echo ========================================
echo Laptop IP: %IP%
echo Port: 1883
echo.
echo Next steps:
echo 1. Configure smartphone with IP: %IP%
echo 2. Ensure both devices on same WiFi network
echo 3. Test connection from smartphone
echo.
echo If issues persist:
echo - Check Mosquitto configuration file
echo - Verify network connectivity
echo - Test with MQTT client app on smartphone
echo ========================================
pause
