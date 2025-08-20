# Complete Setup Guide - ESP32 & MQTT Broker

## 🔧 **ESP32 Setup Instructions**

### **Step 1: Install Required Libraries**

Open Arduino IDE and install these libraries:
1. **ESP32 Board Package**: Tools → Board → Boards Manager → Search "ESP32" → Install
2. **MPU6050 Library**: Sketch → Include Library → Manage Libraries → Search "MPU6050" → Install "MPU6050 by Electronic Cats"
3. **Bluetooth Libraries**: Already included with ESP32 board package

### **Step 2: Configure Arduino IDE**

1. **Select Board**: Tools → Board → ESP32 Arduino → ESP32 Dev Module
2. **Select Port**: Tools → Port → Choose your ESP32 COM port
3. **Upload Speed**: Tools → Upload Speed → 115200

### **Step 3: Upload ESP32 Code**

1. **Open the file**: `ESP32_BLE_TEST.ino` in your project folder
2. **Verify Code**: Click the checkmark (✓) to compile
3. **Upload Code**: Click the arrow (→) to upload to ESP32
4. **Monitor Output**: Tools → Serial Monitor (set to 115200 baud)

### **Step 4: Verify ESP32 Setup**

You should see this output in Serial Monitor:
```
ESP32 Car Crash Detection System Starting...
MPU6050 connection successful
Bluetooth Classic started
Device name: ESP32_CrashDetector
BLE started
Waiting for connections...
```

### **Step 5: Hardware Connections**

Connect MPU6050 to ESP32:
- **VCC** → **3.3V**
- **GND** → **GND**
- **SCL** → **GPIO 22**
- **SDA** → **GPIO 21**

## 📡 **MQTT Broker Setup**

### **Step 1: Install Mosquitto MQTT Broker**

#### **Windows:**
```bash
# Download from: https://mosquitto.org/download/
# Or use Chocolatey:
choco install mosquitto

# Start the service:
net start mosquitto
```

#### **Alternative - Docker:**
```bash
docker run -d --name mosquitto -p 1883:1883 -p 9001:9001 eclipse-mosquitto:latest
```

### **Step 2: Configure Mosquitto**

1. **Copy configuration**: Copy `mosquitto_config.conf` to your Mosquitto installation directory
2. **Restart service**: `net stop mosquitto && net start mosquitto`

### **Step 3: Test MQTT Broker**

Run the test script:
```bash
check_mosquitto.bat
```

You should see:
```
✓ Mosquitto service found
✓ Port 1883 is listening
✓ MQTT connection test successful
```

### **Step 4: Find Your Computer's IP Address**

```bash
# Windows
ipconfig | findstr "IPv4"

# Example output: 192.168.1.100
```

## 📱 **Android App MQTT Configuration**

### **Step 1: Update MQTT Broker URL**

Edit `app/src/main/java/com/example/cc/util/MqttConfig.kt`:

```kotlin
// Change this line to your computer's IP address
const val BROKER_URL = "tcp://YOUR_IP_ADDRESS:1883"
```

Replace `YOUR_IP_ADDRESS` with your computer's actual IP (e.g., `192.168.1.100`).

### **Step 2: Rebuild and Install App**

```bash
# Clean and rebuild
.\gradlew.bat clean
.\gradlew.bat assembleDebug

# Install on connected devices
.\gradlew.bat installDebug
```

## 🔄 **Testing the Complete System**

### **Test 1: MQTT Communication**

1. **Start MQTT Broker**: Ensure Mosquitto is running
2. **Phone 1 (Publisher)**: Open app → Select "Crash Victim" → Enter name
3. **Phone 2 (Subscriber)**: Open app → Select "Emergency Responder" → Enter name
4. **Test Communication**: On Phone 1, press the SOS button
5. **Verify**: Phone 2 should receive the emergency alert

### **Test 2: Bluetooth Discovery**

1. **Power ESP32**: Ensure ESP32 is running with uploaded code
2. **Phone 1**: Open app → Publisher mode → Press "Discover Devices"
3. **Check Logs**: In Android Studio Logcat, look for:
   ```
   BLUETOOTH DISCOVERY DEBUG
   Bluetooth enabled: true
   BLUETOOTH_SCAN permission: GRANTED
   ACCESS_FINE_LOCATION permission: GRANTED
   Found device: ESP32_CrashDetector
   ```

### **Test 3: ESP32 Communication**

1. **Connect to ESP32**: Select "ESP32_CrashDetector" from discovered devices
2. **Test Commands**: Send test commands from Android app
3. **Monitor ESP32**: Check Serial Monitor for received commands

## 🛠️ **Troubleshooting**

### **ESP32 Issues**

#### **Upload Fails**
- Check USB cable and port
- Hold BOOT button during upload
- Try different USB cable

#### **MPU6050 Not Found**
- Check wiring connections
- Verify I2C address (usually 0x68)
- Test with I2C scanner sketch

#### **Bluetooth Not Working**
- Ensure ESP32 has Bluetooth enabled
- Check if device appears in phone's Bluetooth settings
- Verify UUIDs match between ESP32 and Android app

### **MQTT Issues**

#### **Connection Failed**
- Check if Mosquitto is running: `net start mosquitto`
- Verify IP address in MqttConfig.kt
- Check firewall settings (allow port 1883)
- Test with MQTT client: `mosquitto_pub -h localhost -t test -m "hello"`

#### **Messages Not Received**
- Check topic names match
- Verify QoS settings
- Check network connectivity between phones and computer

### **Android App Issues**

#### **Bluetooth Discovery Fails**
- Grant ALL permissions (Location, Bluetooth, etc.)
- Enable Location Services
- Check Android Studio Logcat for detailed error messages

#### **App Crashes**
- Check Logcat for specific error messages
- Verify all dependencies are properly installed
- Try clearing app data and reinstalling

## 📋 **Quick Test Checklist**

- [ ] ESP32 code uploaded successfully
- [ ] ESP32 Serial Monitor shows "Waiting for connections..."
- [ ] Mosquitto MQTT broker running on port 1883
- [ ] Computer IP address updated in MqttConfig.kt
- [ ] Android app rebuilt and installed on both phones
- [ ] All permissions granted on both phones
- [ ] MQTT communication working (SOS button sends alerts)
- [ ] Bluetooth discovery finds ESP32 device
- [ ] ESP32 connection established

## 🎯 **Success Indicators**

### **ESP32 Working**
- Serial Monitor shows continuous sensor data
- Device appears as "ESP32_CrashDetector" in Bluetooth settings
- LED flashes when crash detected

### **MQTT Working**
- Emergency alerts appear on subscriber phone
- No connection errors in Android Studio Logcat
- Mosquitto logs show message activity

### **Bluetooth Working**
- ESP32 appears in device discovery
- Connection established successfully
- Sensor data received in Android app

---

**Need Help?** Check the troubleshooting section above or run `test_bluetooth_setup.bat` for system diagnostics.
