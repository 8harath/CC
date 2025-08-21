@echo off
echo ========================================
echo MQTT Local Broker Communication Test
echo ========================================
echo.
echo Testing MQTT communication with local broker: 192.168.0.101:1883
echo.

echo [1/5] Checking network connectivity to local broker...
ping -n 1 192.168.0.101 >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Local broker reachable: 192.168.0.101
) else (
    echo ❌ Local broker NOT reachable: 192.168.0.101
    echo    Please check:
    echo    1. Mosquitto is running on 192.168.0.101
    echo    2. Port 1883 is open
    echo    3. Firewall allows MQTT traffic
    echo    4. Both devices are on same network
)

echo.
echo [2/5] Checking MQTT broker port connectivity...
powershell -Command "try { $socket = New-Object System.Net.Sockets.TcpClient; $socket.Connect('192.168.0.101', 1883); $socket.Close(); Write-Host '✅ MQTT port 1883 accessible'; } catch { Write-Host '❌ MQTT port 1883 NOT accessible' }"

echo.
echo [3/5] Checking Android ADB connection...
adb devices
echo.

echo [4/5] Checking app installation...
adb shell pm list packages | findstr "com.example.cc"
if %errorlevel% equ 0 (
    echo ✅ Car Crash Detection app: INSTALLED
) else (
    echo ❌ Car Crash Detection app: NOT FOUND
    echo    Please install the updated app first
)

echo.
echo [5/5] Testing MQTT communication...
echo.
echo === TESTING INSTRUCTIONS ===
echo.
echo 1. INSTALL UPDATED APP:
echo    - Uninstall old app from both devices
echo    - Install the new APK on both devices
echo.
echo 2. SETUP PUBLISHER (Phone 1):
echo    - Open app → Emergency Alert Publisher
echo    - App will automatically enable MQTT
echo    - Wait for "MQTT: Connected" status
echo    - Verify "Send Simple Message" button is enabled
echo.
echo 3. SETUP SUBSCRIBER (Phone 2):
echo    - Open app → Emergency Responder
echo    - App will automatically enable MQTT
echo    - Wait for "MQTT: Connected" status
echo    - Verify "Test Connection" button is enabled
echo.
echo 4. TEST COMMUNICATION:
echo    - Publisher: Tap "Send Simple Message"
echo    - Subscriber: Should receive notification immediately
echo    - Check alert history for received message
echo.
echo === TROUBLESHOOTING ===
echo.
echo If MQTT still doesn't connect:
echo 1. Verify Mosquitto is running on 192.168.0.101
echo 2. Check firewall settings on 192.168.0.101
echo 3. Ensure both devices are on same WiFi network
echo 4. Try restarting both devices
echo 5. Check app logs: adb logcat | grep -i mqtt
echo.
echo === MOSQUITTO SETUP ===
echo.
echo On the device with IP 192.168.0.101:
echo 1. Install Mosquitto: https://mosquitto.org/download/
echo 2. Start service: net start mosquitto
echo 3. Verify it's running: netstat -an | findstr 1883
echo 4. Test locally: mosquitto_pub -h localhost -t "test" -m "test"
echo.
echo ========================================
echo Test complete. Follow the instructions above.
echo ========================================
pause
