# MQTT Setup and Testing Guide for ESP32 Car Crash Detection System

This guide will help you set up and test MQTT communication between your ESP32 and a local Mosquitto broker.

## Prerequisites

1. **ESP32 Development Board** with WiFi capability
2. **MPU6050 Accelerometer** connected to ESP32
3. **Local Mosquitto MQTT Broker** installed and running
4. **Python** with paho-mqtt library installed
5. **Arduino IDE** with ESP32 board support

## Step 1: Install Mosquitto MQTT Broker

### Windows:
1. Download Mosquitto from: https://mosquitto.org/download/
2. Install the Windows installer
3. Start Mosquitto service:
   ```cmd
   net start mosquitto
   ```

### Linux (Ubuntu/Debian):
```bash
sudo apt update
sudo apt install mosquitto mosquitto-clients
sudo systemctl enable mosquitto
sudo systemctl start mosquitto
```

### macOS:
```bash
brew install mosquitto
brew services start mosquitto
```

## Step 2: Install Required Libraries

### Arduino IDE Libraries:
1. Open Arduino IDE
2. Go to **Tools > Manage Libraries**
3. Install the following libraries:
   - **PubSubClient** by Nick O'Leary
   - **MPU6050** by Electronic Cats (if not already installed)

### Python Libraries:
```bash
pip install paho-mqtt
```

## Step 3: Configure ESP32 Code

1. **Update WiFi Credentials**: In `ESP32_BLE_TEST.ino`, modify these lines:
   ```cpp
   const char* ssid = "YOUR_WIFI_SSID";  // Replace with your WiFi SSID
   const char* password = "YOUR_WIFI_PASSWORD";  // Replace with your WiFi password
   ```

2. **Update MQTT Broker IP**: Set the correct IP address of your Mosquitto broker:
   ```cpp
   const char* mqtt_server = "192.168.1.100";  // Replace with your broker IP
   ```
   - If running on the same machine as your development environment, use `"localhost"` or `"127.0.0.1"`
   - If running on a different machine, use the actual IP address

3. **Upload the Code**: Upload the modified code to your ESP32

## Step 4: Test MQTT Communication

### Method 1: Using the Python Test Script

1. **Run the test script**:
   ```bash
   python mqtt_test.py
   ```

2. **Follow the interactive menu**:
   - Option 1: Send MQTT test message
   - Option 2: Send crash test command
   - Option 3: Send status request
   - Option 4: Exit

### Method 2: Using Mosquitto Command Line Tools

1. **Subscribe to ESP32 topics**:
   ```bash
   mosquitto_sub -h localhost -t "esp32/#" -v
   ```

2. **Publish test messages** (in another terminal):
   ```bash
   mosquitto_pub -h localhost -t "esp32/command" -m "MQTT_TEST"
   mosquitto_pub -h localhost -t "esp32/command" -m "TEST_CRASH"
   ```

### Method 3: Using MQTT Explorer (GUI Tool)

1. Download MQTT Explorer from: https://mqtt-explorer.com/
2. Connect to your local Mosquitto broker
3. Subscribe to topics: `esp32/#`
4. Publish messages to: `esp32/command`

## Step 5: Expected Behavior

### When ESP32 Starts:
- Connects to WiFi
- Connects to MQTT broker
- Publishes status message: `"ESP32 Crash Detector Online"`

### Sensor Data:
- Published every 5 seconds to topic: `esp32/sensor_data`
- Format: `SENSOR:ACCEL:x.xxx,y.yyy,z.zzz|IMPACT:x.xxx|TIME:xxxxx`

### Crash Detection:
- Published immediately to topic: `esp32/crash_alert`
- Format: `CRASH:SEVERITY:HIGH|IMPACT:x.xxx|ACCEL:x.xxx,y.yyy,z.zzz|TIME:xxxxx`

### Test Messages:
- Published to topic: `esp32/test`
- Content: `"Hello from ESP32! MQTT test successful!"`

## Troubleshooting

### ESP32 Won't Connect to WiFi:
- Check WiFi credentials
- Ensure ESP32 is in range
- Check Serial Monitor for connection status

### ESP32 Won't Connect to MQTT Broker:
- Verify Mosquitto is running: `mosquitto_sub -h localhost -t "test" -v`
- Check broker IP address
- Ensure ESP32 and broker are on same network
- Check firewall settings

### No Messages Received:
- Verify topics are correct
- Check MQTT client ID conflicts
- Ensure proper subscription to topics
- Check Serial Monitor for ESP32 debug messages

### Common Error Codes:
- `-2`: Connection refused (broker not running)
- `-3`: Server unavailable (wrong IP/port)
- `-4`: Bad username/password (if authentication enabled)

## Advanced Configuration

### Enable MQTT Authentication:
1. Create password file:
   ```bash
   mosquitto_passwd -c /etc/mosquitto/passwd username
   ```

2. Update mosquitto.conf:
   ```conf
   allow_anonymous false
   password_file /etc/mosquitto/passwd
   ```

3. Update ESP32 code with credentials:
   ```cpp
   const char* mqtt_username = "username";
   const char* mqtt_password = "password";
   ```

### Enable SSL/TLS:
1. Generate certificates
2. Configure mosquitto.conf for SSL
3. Update ESP32 code to use port 8883

## Testing Scenarios

1. **Basic Connectivity**: Verify ESP32 connects to broker
2. **Sensor Data**: Check regular sensor data transmission
3. **Crash Simulation**: Trigger crash detection and verify alert
4. **Command Response**: Send commands and verify ESP32 response
5. **Network Interruption**: Test reconnection after network loss

## Next Steps

Once MQTT is working:
1. Integrate with your Android app
2. Add data logging to database
3. Implement alert notifications
4. Add more sensor types
5. Implement data analytics

## Support

If you encounter issues:
1. Check Serial Monitor for ESP32 debug messages
2. Verify Mosquitto broker logs
3. Test with simple MQTT clients first
4. Ensure all network connectivity is working
