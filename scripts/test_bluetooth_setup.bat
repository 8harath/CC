@echo off
echo ========================================
echo Bluetooth Setup Test Script
echo ========================================
echo.

echo 1. Checking Bluetooth adapter status...
powershell "Get-PnpDevice | Where-Object {$_.FriendlyName -like '*Bluetooth*'} | Select-Object FriendlyName, Status"
echo.

echo 2. Checking for ESP32 devices in range...
echo    (This will show nearby Bluetooth devices)
echo    Look for devices named "ESP32_CrashDetector" or similar
echo.
powershell "Get-PnpDevice | Where-Object {$_.FriendlyName -like '*ESP32*' -or $_.FriendlyName -like '*Bluetooth*'} | Select-Object FriendlyName, Status"
echo.

echo 3. Android App Bluetooth Test Instructions:
echo    a) Open the Car Crash Detection app on both phones
echo    b) On Phone 1 (Publisher): Go to Publisher mode
echo    c) Press "Discover Devices" button
echo    d) Check Android Studio Logcat for debug messages
echo    e) Look for "BLUETOOTH DISCOVERY DEBUG" messages
echo.

echo 4. ESP32 Test Instructions:
echo    a) Upload ESP32_BLE_TEST.ino to your ESP32
echo    b) Open Serial Monitor (115200 baud)
echo    c) You should see: "ESP32 Car Crash Detection System Starting..."
echo    d) Look for: "Bluetooth Classic started" and "BLE started"
echo    e) Device should appear as "ESP32_CrashDetector"
echo.

echo 5. Troubleshooting Steps:
echo    - Make sure ESP32 is powered on
echo    - Ensure Bluetooth is enabled on both phones
echo    - Grant ALL permissions when prompted by the app
echo    - Enable Location Services (required for Bluetooth scanning)
echo    - Check that ESP32 is not already connected to another device
echo.

echo 6. Common Issues:
echo    - "No devices found": Check permissions and location services
echo    - "Connection failed": ESP32 might be connected to another device
echo    - "Permission denied": Grant all requested permissions
echo.

echo ========================================
echo Test Complete
echo ========================================
pause
