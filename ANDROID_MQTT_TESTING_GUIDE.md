# Android MQTT Testing Guide

This guide will help you test MQTT communication between two Android devices using the Car Crash Detection app.

## Overview

The app now includes MQTT testing functionality in both Publisher and Subscriber modes:

- **Publisher Mode**: Can send test messages to MQTT topics
- **Subscriber Mode**: Can receive and display messages from MQTT topics

## Prerequisites

1. **Two Android devices** (or one device + emulator)
2. **Local Mosquitto MQTT Broker** running on your network
3. **Car Crash Detection App** installed on both devices
4. **Network connectivity** between devices and MQTT broker

## Step 1: Setup MQTT Broker

### Install Mosquitto (if not already installed):

**Windows:**
```cmd
# Download from https://mosquitto.org/download/
# Install and start service
net start mosquitto
```

**Linux:**
```bash
sudo apt install mosquitto mosquitto-clients
sudo systemctl start mosquitto
```

**macOS:**
```bash
brew install mosquitto
brew services start mosquitto
```

### Verify Broker is Running:
```bash
mosquitto_sub -h localhost -t "test" -v
```

## Step 2: Configure App Settings

### Update MQTT Configuration:

In the app's MQTT service, ensure the broker IP is correct:

```kotlin
// In MqttConfig.kt or similar
const val BROKER_URL = "tcp://YOUR_BROKER_IP:1883"
```

Replace `YOUR_BROKER_IP` with:
- `localhost` or `127.0.0.1` if testing on same machine
- Your actual broker IP address if on different machine

## Step 3: Testing Process

### Device 1: Publisher Mode

1. **Open the app** and select **"Emergency Alert Publisher"**
2. **Enable MQTT** by tapping the "Enable" button in the Connection Status section
3. **Wait for connection** - Status should show "MQTT: Connected"
4. **Test MQTT Connection** by tapping "Test MQTT Connection"
   - Should show: "✅ MQTT Connection Test: SUCCESS"
5. **Send Test Message** by tapping "Send Test Message"
   - Should show: "✅ Test message sent to 'emergency/test' topic"

### Device 2: Subscriber Mode

1. **Open the app** and select **"Emergency Response Dashboard"**
2. **Enable MQTT** by tapping the "Enable MQTT" button
3. **Wait for connection** - Status should show "MQTT: Connected"
4. **Test MQTT Connection** by tapping "Test Connection"
   - Should show: "✅ MQTT Connection Test: SUCCESS"
5. **Check Received Messages** by tapping "Check Messages"
   - Should show received message count and details

## Step 4: Message Flow Testing

### Test 1: Basic Message Sending

1. **Publisher Device**: Tap "Send Test Message"
2. **Subscriber Device**: Tap "Check Messages"
3. **Expected Result**: Subscriber should show received message

### Test 2: Connection Status

1. **Both Devices**: Tap "Test MQTT Connection"
2. **Expected Result**: Both should show "SUCCESS" status

### Test 3: Real-time Message Reception

1. **Publisher Device**: Send multiple test messages
2. **Subscriber Device**: Check messages after each send
3. **Expected Result**: Message count should increase

## Step 5: Advanced Testing

### Using Command Line Tools:

**Subscribe to all emergency topics:**
```bash
mosquitto_sub -h localhost -t "emergency/#" -v
```

**Publish test message manually:**
```bash
mosquitto_pub -h localhost -t "emergency/test" -m '{"type":"test","message":"Hello from command line"}'
```

### Using MQTT Explorer (GUI):

1. Download MQTT Explorer from https://mqtt-explorer.com/
2. Connect to your local broker
3. Subscribe to `emergency/#` topic
4. Monitor messages in real-time

## Troubleshooting

### Connection Issues:

**"MQTT Connection Test: FAILED"**
- Check if Mosquitto broker is running
- Verify broker IP address in app configuration
- Ensure devices are on same network as broker
- Check firewall settings

**"No messages received"**
- Verify both devices are connected to MQTT
- Check topic names match between publisher and subscriber
- Ensure QoS settings are compatible

### Common Error Messages:

**"Connection refused"**
- Broker not running or wrong IP address

**"Network unavailable"**
- Check WiFi connectivity
- Verify network permissions

**"Authentication failed"**
- Check username/password if authentication is enabled

## Expected Message Format

The app sends test messages in JSON format:

```json
{
    "type": "test_message",
    "sender": "publisher_phone",
    "timestamp": "1234567890",
    "message": "Hello from Publisher! This is a test message.",
    "location": {
        "latitude": 0.0,
        "longitude": 0.0
    }
}
```

## Testing Scenarios

### Scenario 1: Basic Communication
- ✅ Publisher sends message
- ✅ Subscriber receives message
- ✅ Connection status shows "Connected"

### Scenario 2: Multiple Messages
- ✅ Publisher sends multiple messages
- ✅ Subscriber receives all messages
- ✅ Message count increases correctly

### Scenario 3: Network Interruption
- ✅ Disconnect one device from network
- ✅ Reconnect device
- ✅ MQTT reconnects automatically
- ✅ Messages resume flowing

### Scenario 4: Emergency Alert Flow
- ✅ Publisher sends emergency alert
- ✅ Subscriber receives emergency alert
- ✅ Alert appears in subscriber's alert list

## Success Criteria

Your MQTT testing is successful when:

1. **Both devices connect** to MQTT broker successfully
2. **Publisher can send** test messages
3. **Subscriber can receive** and display messages
4. **Connection status** shows "Connected" on both devices
5. **Real-time communication** works between devices

## Next Steps

Once MQTT testing is working:

1. **Test emergency alerts** between devices
2. **Add more message types** (location updates, status updates)
3. **Implement message persistence** for offline scenarios
4. **Add message encryption** for security
5. **Scale to multiple devices** for real-world testing

## Support

If you encounter issues:

1. Check the app logs for detailed error messages
2. Verify Mosquitto broker logs
3. Test with command-line MQTT tools first
4. Ensure all network connectivity is working
5. Check app permissions for network access
