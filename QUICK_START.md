# Quick Start Guide - Android MQTT Application

## 🚀 Getting Started

This guide will help you quickly set up and run the Android MQTT application with both publishing and subscribing capabilities.

## Prerequisites

- **Android Studio** (latest version)
- **Android Device** or **Emulator** (API level 24+)
- **Mosquitto MQTT Broker** running on your laptop
- **Python 3** (for testing script)

## 📱 Building the Application

### 1. Open the Project
```bash
# Clone or open the project in Android Studio
cd CC
```

### 2. Build the APK
```bash
# Build debug APK
./gradlew assembleDebug

# Or use the provided script
./scripts/build_apk.bat  # Windows
./scripts/build_apk.sh   # Linux/macOS
```

### 3. Install on Device
```bash
# Install via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Or transfer APK to device and install manually
```

## 🔧 Setting Up MQTT Broker

### 1. Install Mosquitto

**Windows:**
- Download from: https://mosquitto.org/download/
- Install and start the service

**macOS:**
```bash
brew install mosquitto
```

**Linux:**
```bash
sudo apt-get install mosquitto mosquitto-clients
```

### 2. Start Mosquitto
```bash
# Start on port 1883
mosquitto -p 1883

# Or start as a service
sudo systemctl start mosquitto
```

### 3. Test Broker Setup
```bash
# Run the test script
python3 scripts/test_mqtt_setup.py

# Or test manually
mosquitto_pub -h localhost -t "test/topic" -m "Hello World"
mosquitto_sub -h localhost -t "test/topic"
```

## 📱 Using the Application

### 1. Launch the App
- Open the Android application
- You'll see the role selection screen

### 2. Configure Broker Settings
- Select either **Publisher** or **Subscriber** mode
- Enter your laptop's IP address (e.g., `192.168.1.100`)
- Enter port number: `1883`
- Tap **Save Settings**

### 3. Test Connection
- Tap **Test Connection**
- The app will perform multiple validation tests
- Check the connection indicator (green = connected)

### 4. Publisher Mode
- Enter a custom message (optional)
- Tap **Send Emergency Alert**
- Message will be published to `emergency/alerts/alert`

### 5. Subscriber Mode
- App automatically subscribes to emergency topics
- Incoming messages will appear in real-time
- Use **Send Test Message** to verify communication

## 🔍 Testing Communication

### Test with Two Devices
1. **Device A (Publisher):**
   - Select Publisher mode
   - Configure broker settings
   - Send test messages

2. **Device B (Subscriber):**
   - Select Subscriber mode
   - Configure same broker settings
   - Watch for incoming messages

### Test with Command Line
```bash
# Subscribe to emergency topics
mosquitto_sub -h <your-laptop-ip> -t "emergency/#"

# Send test message
mosquitto_pub -h <your-laptop-ip> -t "emergency/alerts/alert" -m '{"type":"test","message":"Hello from command line"}'
```

## 🛠️ Troubleshooting

### Common Issues

**"Connection Failed"**
- Check if Mosquitto is running: `pgrep mosquitto`
- Verify IP address and port
- Check firewall settings
- Test with: `telnet <your-ip> 1883`

**"Invalid IP Address"**
- Ensure correct IP format (e.g., 192.168.1.100)
- Check for typos
- Verify network configuration

**"Network Not Available"**
- Check WiFi connection
- Ensure device is on same network as broker
- Test internet connectivity

### Debug Commands
```bash
# Check application logs
adb logcat | grep -E "(MqttService|MqttConfig|NetworkHelper)"

# Test network connectivity
ping <broker-ip>
telnet <broker-ip> 1883

# Check Mosquitto logs
tail -f /var/log/mosquitto/mosquitto.log
```

## 📋 Key Features

### ✅ **Connection Validation**
- Multi-layer connectivity testing
- Real-time status monitoring
- Automatic reconnection
- Meaningful error messages

### ✅ **Message Handling**
- Custom message input
- Real-time message display
- Message queuing when disconnected
- Topic validation

### ✅ **User Interface**
- Clean, intuitive design
- Role-based workflows
- Real-time status indicators
- Comprehensive error feedback

## 🔧 Advanced Configuration

### MQTT Topics
- `emergency/alerts/alert` - Emergency alerts
- `emergency/alerts/#` - All emergency alerts
- `emergency/test/#` - Test messages
- `emergency/custom/#` - Custom messages

### Settings
- Broker IP and port configuration
- Connection timeout settings
- Keep-alive interval
- Authentication (if configured)

## 📚 Additional Resources

- **Full Documentation**: `docs/MQTT_IMPLEMENTATION_GUIDE.md`
- **Troubleshooting**: `docs/TROUBLESHOOTING.md`
- **Development Guide**: `docs/DEVELOPMENT_PLAN.md`

## 🎯 Success Criteria

You'll know everything is working when:

1. ✅ App connects to broker successfully
2. ✅ Publisher can send messages
3. ✅ Subscriber receives messages in real-time
4. ✅ Connection status shows "Connected" (green)
5. ✅ Test messages are exchanged between devices

## 🚀 Next Steps

- Explore advanced features in the app
- Configure authentication if needed
- Set up persistent message storage
- Integrate with other MQTT clients

---

**Need Help?** Check the troubleshooting section or review the detailed documentation in the `docs/` folder.
