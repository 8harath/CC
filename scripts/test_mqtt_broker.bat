@echo off
echo ========================================
echo MQTT Broker Connection Test
echo ========================================
echo.
echo Testing connection to MQTT broker at 10.0.0.208:1883
echo.

REM Test basic network connectivity
echo [1/4] Testing network connectivity...
ping -n 1 10.0.0.208 >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Network connectivity: OK
) else (
    echo ❌ Network connectivity: FAILED
    echo Please check if the broker laptop is running and accessible
    pause
    exit /b 1
)

REM Test port connectivity
echo.
echo [2/4] Testing port connectivity...
powershell -Command "try { $tcp = New-Object System.Net.Sockets.TcpClient; $tcp.Connect('10.0.0.208', 1883); $tcp.Close(); Write-Host '✅ Port 1883: OPEN' } catch { Write-Host '❌ Port 1883: CLOSED or BLOCKED' }"

REM Test Python MQTT client if available
echo.
echo [3/4] Testing Python MQTT client...
python --version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Python found, testing MQTT connection...
    python test_mqtt_broker.py
) else (
    echo ⚠️ Python not found, skipping MQTT client test
    echo To test MQTT functionality, install Python and run: python test_mqtt_broker.py
)

echo.
echo [4/4] Testing complete!
echo.
echo If all tests passed, your MQTT broker should be working correctly.
echo You can now test the Android app's publisher and subscriber functionality.
echo.
pause
