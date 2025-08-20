@echo off
echo ========================================
echo MQTT Communication Diagnostic Tool
echo ========================================
echo.

echo [1/6] Checking network connectivity...
ping -n 1 8.8.8.8 >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Internet connectivity: OK
) else (
    echo ❌ Internet connectivity: FAILED
    echo    Please check your internet connection
)

echo.
echo [2/6] Checking MQTT broker connectivity...
ping -n 1 broker.hivemq.com >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ MQTT broker reachable: OK
) else (
    echo ❌ MQTT broker reachable: FAILED
    echo    Cannot reach broker.hivemq.com
)

echo.
echo [3/6] Checking local network...
ipconfig | findstr "IPv4"
echo.

echo [4/6] Checking Android ADB connection...
adb devices
echo.

echo [5/6] Checking app installation...
adb shell pm list packages | findstr "com.example.cc"
if %errorlevel% equ 0 (
    echo ✅ Car Crash Detection app: INSTALLED
) else (
    echo ❌ Car Crash Detection app: NOT FOUND
    echo    Please install the app first
)

echo.
echo [6/6] Checking app logs for MQTT errors...
echo.
echo === Recent MQTT-related logs ===
adb logcat -d | findstr /i "mqtt\|MqttService\|PublisherActivity\|SubscriberActivity" | findstr /i "error\|failed\|exception" | tail -10
echo.

echo ========================================
echo DIAGNOSTIC SUMMARY
echo ========================================
echo.
echo TROUBLESHOOTING STEPS:
echo.
echo 1. NETWORK ISSUES:
echo    - Ensure both devices are on the same WiFi network
echo    - Check that internet connectivity is working
echo    - Try disabling firewall temporarily
echo.
echo 2. MQTT CONNECTION:
echo    - Open app on both devices
echo    - Go to MQTT Settings
echo    - Set broker to: tcp://broker.hivemq.com:1883
echo    - Press "Enable MQTT" on both devices
echo    - Wait for "MQTT: Connected" status
echo.
echo 3. MESSAGE TESTING:
echo    - Publisher: Press "Send Simple Message"
echo    - Subscriber: Check for notification and alert history
echo    - Use "Test Connection" button to verify connectivity
echo.
echo 4. LOG ANALYSIS:
echo    - Check Android logs for MQTT errors
echo    - Look for connection state changes
echo    - Monitor message publish/receive events
echo.
echo 5. COMMON FIXES:
echo    - Restart both devices
echo    - Clear app data and cache
echo    - Reinstall the app
echo    - Try different MQTT broker
echo.
echo ========================================
echo Diagnostic complete. Check the results above.
echo ========================================
pause
