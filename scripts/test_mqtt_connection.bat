@echo off
echo ========================================
echo MQTT Connection Test Script
echo ========================================
echo.

echo Checking if Mosquitto broker is running...
netstat -an | findstr :1883
if %errorlevel% equ 0 (
    echo ✅ Mosquitto broker is running on port 1883
) else (
    echo ❌ Mosquitto broker is NOT running on port 1883
    echo Please start Mosquitto broker first
)
echo.

echo Checking your local IP address...
ipconfig | findstr "IPv4"
echo.

echo Testing common MQTT broker URLs...
echo.

echo Testing localhost:1883...
powershell -Command "try { $socket = New-Object System.Net.Sockets.TcpClient; $socket.Connect('localhost', 1883); $socket.Close(); Write-Host '✅ localhost:1883 - SUCCESS' } catch { Write-Host '❌ localhost:1883 - FAILED' }"

echo Testing 192.168.1.100:1883...
powershell -Command "try { $socket = New-Object System.Net.Sockets.TcpClient; $socket.Connect('192.168.1.100', 1883); $socket.Close(); Write-Host '✅ 192.168.1.100:1883 - SUCCESS' } catch { Write-Host '❌ 192.168.1.100:1883 - FAILED' }"

echo Testing 192.168.0.100:1883...
powershell -Command "try { $socket = New-Object System.Net.Sockets.TcpClient; $socket.Connect('192.168.0.100', 1883); $socket.Close(); Write-Host '✅ 192.168.0.100:1883 - SUCCESS' } catch { Write-Host '❌ 192.168.0.100:1883 - FAILED' }"

echo.
echo ========================================
echo Test Complete
echo ========================================
echo.
echo If you see SUCCESS messages above, your MQTT broker is accessible.
echo Use the successful IP address in your Android app's MQTT settings.
echo.
pause
