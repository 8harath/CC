# Android MQTT Testing Guide

## Overview
This guide explains how to set up and test MQTT communication between the Android app and a local Mosquitto broker running on your laptop.

## Prerequisites
1. **Mosquitto Broker** installed and running on your laptop
2. **Two Android devices** (or one device + emulator) for testing publisher/subscriber
3. **Same WiFi network** for all devices (laptop + phones)

## Step 1: Set Up Mosquitto Broker on Laptop

### Install Mosquitto (Windows)
```bash
# Download from: https://mosquitto.org/download/
# Or use chocolatey:
choco install mosquitto
```

### Install Mosquitto (macOS)
```bash
brew install mosquitto
```

### Install Mosquitto (Linux)
```bash
sudo apt-get install mosquitto mosquitto-clients
```

### Start Mosquitto Broker
```bash
# Start the broker (it will run on port 1883 by default)
mosquitto

# Or start as a service
sudo systemctl start mosquitto
```

### Verify Broker is Running
```bash
# Test if broker is listening on port 1883
netstat -an | grep 1883
# Should show: tcp 0 0 0.0.0.0:1883 0.0.0.0:* LISTEN
```

## Step 2: Find Your Laptop's IP Address

### Windows
```cmd
ipconfig
# Look for "IPv4 Address" under your WiFi adapter
```

### macOS/Linux
```bash
ifconfig
# Look for "inet" followed by your IP address
```

**Note down your laptop's IP address** (e.g., `192.168.1.100`)

## Step 3: Configure MQTT Settings in Android App

### Open MQTT Settings
1. Launch the Android app
2. Navigate to **MQTT Settings** (from main menu or publisher/subscriber screens)

### Configure Broker Settings
1. **Enter your laptop's IP address** in the "Broker IP Address" field
   - Use the IP address you found in Step 2
   - Example: `192.168.1.100`
2. **Set port to 1883** (default MQTT port)
3. **Click "Test Connection"** to verify connectivity
   - Should show: "✅ MQTT Connection successful"
4. **Click "Save Settings"** to save the configuration

### Enable MQTT Service
1. **Click "Enable MQTT Service"** in the MQTT Settings
2. This will start the MQTT service and connect to your broker
3. The button should change to "MQTT Service Enabled" (green)

## Step 4: Test Publisher Functionality

### Set Up Publisher Device
1. **Install the app** on the first Android device
2. **Open MQTT Settings** and configure with your laptop's IP
3. **Enable MQTT Service**
4. **Navigate to Publisher screen**

### Enable MQTT in Publisher
1. **Click "Enable MQTT"** button
2. Status should change to "MQTT: Connected"
3. **Test buttons should become enabled**

### Send Test Message
1. **Click "Send Test Message"**
2. Should show: "✅ Test message sent to 'emergency/test' topic"
3. **Check MQTT logs** on your laptop to see the message

## Step 5: Test Subscriber Functionality

### Set Up Subscriber Device
1. **Install the app** on the second Android device
2. **Open MQTT Settings** and configure with your laptop's IP
3. **Enable MQTT Service**
4. **Navigate to Subscriber screen**

### Enable MQTT in Subscriber
1. **Click "Enable MQTT"** button
2. Status should change to "MQTT: Connected"
3. **Test buttons should become enabled**

### Receive Messages
1. **Click "Check Received Messages"** to see current status
2. **Send a test message from Publisher**
3. **Check if message appears** in Subscriber's alert list

## Step 6: Monitor MQTT Traffic

### On Your Laptop (Optional)
```bash
# Subscribe to all emergency topics to monitor traffic
mosquitto_sub -h localhost -t "emergency/#" -v

# This will show all messages published to emergency topics
```

### In Android App
- **Publisher**: Check "Test MQTT Connection" button for status
- **Subscriber**: Check "Check Received Messages" for received alerts

## Troubleshooting

### Connection Issues
1. **Verify broker is running**:
   ```bash
   # Check if mosquitto is running
   ps aux | grep mosquitto
   ```

2. **Check firewall settings**:
   - Ensure port 1883 is open on your laptop
   - Windows: Check Windows Firewall
   - macOS: Check System Preferences > Security & Privacy

3. **Verify network connectivity**:
   - All devices must be on the same WiFi network
   - Try pinging your laptop's IP from the phone

### App Issues
1. **MQTT Service not connecting**:
   - Check IP address is correct
   - Ensure MQTT service is enabled in settings
   - Restart the app

2. **Messages not received**:
   - Verify both devices are connected
   - Check if subscriber is listening to correct topics
   - Try sending test message again

3. **App crashes**:
   - Check logcat for error messages
   - Ensure all permissions are granted

## Testing Scenarios

### Scenario 1: Basic Message Exchange
1. Set up Publisher and Subscriber
2. Send test message from Publisher
3. Verify message received in Subscriber

### Scenario 2: Emergency Alert Simulation
1. In Publisher, simulate a car crash detection
2. Send emergency alert with location data
3. Verify alert appears in Subscriber's emergency list

### Scenario 3: Multiple Subscribers
1. Set up multiple subscriber devices
2. Send message from Publisher
3. Verify all subscribers receive the message

### Scenario 4: Network Interruption
1. Disconnect Publisher from WiFi
2. Try to send message (should queue)
3. Reconnect WiFi
4. Verify message is sent when connection restored

## Expected Behavior

### Publisher
- ✅ MQTT status shows "Connected"
- ✅ Test message sends successfully
- ✅ Emergency alerts are published to broker

### Subscriber
- ✅ MQTT status shows "Connected"
- ✅ Receives messages from Publisher
- ✅ Alert list updates with new messages
- ✅ Can view message details

### Broker (Laptop)
- ✅ Accepts connections from both devices
- ✅ Routes messages between Publisher and Subscriber
- ✅ Logs show message traffic

## Success Indicators

When everything is working correctly, you should see:

1. **Publisher**: "✅ Test message sent to 'emergency/test' topic"
2. **Subscriber**: New alert appears in the list
3. **Broker logs**: Show connection and message activity
4. **Both devices**: MQTT status shows "Connected"

## Next Steps

Once basic MQTT communication is working:

1. **Test emergency scenarios** with real sensor data
2. **Implement message persistence** for offline scenarios
3. **Add message encryption** for security
4. **Scale to multiple devices** for real-world testing

## Support

If you encounter issues:
1. Check the troubleshooting section above
2. Verify all prerequisites are met
3. Check Android logcat for detailed error messages
4. Ensure Mosquitto broker is properly configured and running
