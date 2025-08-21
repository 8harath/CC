# MQTT Publisher-Subscriber Communication Demo

## Overview
This guide demonstrates how to test the MQTT communication between publisher and subscriber devices in the Car Crash Detection app.

## Prerequisites
- Two Android devices (or one device and an emulator)
- Both devices connected to the same network (WiFi recommended)
- Internet connectivity (for public MQTT broker)

## Setup Instructions

### Step 1: Configure MQTT Settings
1. Open the app on both devices
2. Go to **MQTT Settings** in both apps
3. Configure the same broker settings:
   - **Broker URL**: `tcp://broker.hivemq.com:1883` (public broker for testing)
   - **Username**: (leave empty for public broker)
   - **Password**: (leave empty for public broker)
   - **Client ID**: (auto-generated)

### Step 2: Enable MQTT on Both Devices

#### Publisher Device (Phone 1):
1. Open the **Emergency Alert Publisher** mode
2. Tap **"Enable MQTT"** button
3. Wait for connection status to show **"MQTT: Connected"**
4. Verify the **"Send Test Message"** and **"Send Simple Message"** buttons are enabled

#### Subscriber Device (Phone 2):
1. Open the **Emergency Responder** mode
2. Tap **"Enable MQTT"** button
3. Wait for connection status to show **"MQTT: Connected"**
4. Verify the **"Test Connection"** and **"Check Messages"** buttons are enabled

## Testing Communication

### Test 1: Simple Message Communication
1. **On Publisher Device:**
   - Tap **"Send Simple Message"** button
   - You should see a success toast: "✅ Message sent successfully to emergency/test/message"

2. **On Subscriber Device:**
   - You should receive a notification: "📨 Test Message Received"
   - The message will appear in the alert history list
   - Tap **"Check Messages"** to see the count of received messages

### Test 2: Emergency Alert Communication
1. **On Publisher Device:**
   - Tap **"Send Test Message"** button
   - This sends a full emergency alert with medical information

2. **On Subscriber Device:**
   - You should receive a high-priority emergency notification
   - The alert will appear in the emergency alerts list
   - Tap on the alert to view detailed information

### Test 3: Connection Testing
1. **On Both Devices:**
   - Tap **"Test Connection"** button
   - You should see: "✅ MQTT Connection Test: SUCCESS"

## Troubleshooting

### Connection Issues
- **Problem**: "MQTT: Disconnected" or "Failed to connect"
- **Solutions**:
  1. Ensure both devices are on the same WiFi network
  2. Check internet connectivity (required for public broker)
  3. Try using a different MQTT broker in settings
  4. Restart the app and try again
  5. Check firewall settings on your network

### Message Not Received
- **Problem**: Publisher sends message but subscriber doesn't receive it
- **Solutions**:
  1. Verify both devices show "MQTT: Connected" status
  2. Check that subscriber is in the correct mode (Emergency Responder)
  3. Ensure notification permissions are granted
  4. Look at the app logs for error messages
  5. Try sending a simple message first, then emergency alert

### Message Not Sent
- **Problem**: Publisher button doesn't send messages
- **Solutions**:
  1. Ensure MQTT is enabled and connected
  2. Check that the "Enable MQTT" button was pressed
  3. Verify the connection status shows "MQTT: Connected"
  4. Try the "Test Connection" button first
  5. Check app logs for publish errors

### Common Error Messages
- **"MQTT: Disconnected"**: Network or broker issues
- **"Failed to connect"**: Check broker URL and credentials
- **"Message not delivered"**: Check QoS settings and network stability
- **"MQTT not enabled"**: Press the "Enable MQTT" button first

## Advanced Testing

### Custom Messages
You can modify the message content in the code:
- **Publisher**: Edit `sendSimpleTestMessage()` in `PublisherViewModel.kt`
- **Subscriber**: Check received messages in the alert history

### Multiple Subscribers
- You can have multiple devices in subscriber mode
- All subscribers will receive the same messages
- Each subscriber can respond independently

### Offline Testing
- Messages are queued when offline
- They will be sent when connection is restored
- Check the message queue status in logs

## Logs and Debugging
- Enable verbose logging in the app
- Check Android logs for MQTT-related messages
- Look for connection state changes
- Monitor message delivery confirmations
- Key log tags to watch:
  - `MqttService`: Connection and message handling
  - `PublisherActivity`: UI and user interactions
  - `SubscriberActivity`: Message reception and display

## Step-by-Step Debugging

### If Nothing Works:
1. **Check Network**: Ensure both devices are on WiFi
2. **Check Internet**: Test web browsing on both devices
3. **Enable MQTT**: Press "Enable MQTT" on both devices
4. **Test Connection**: Use "Test Connection" button
5. **Send Simple Message**: Try the simple message first
6. **Check Logs**: Look for error messages in Android logs

### If Messages Don't Send:
1. **Check MQTT Status**: Should show "MQTT: Connected"
2. **Check Buttons**: Send buttons should be enabled
3. **Check Logs**: Look for publish errors
4. **Try Different Message**: Test both simple and emergency messages

### If Messages Don't Receive:
1. **Check Subscriber Mode**: Should be in "Emergency Responder"
2. **Check Notifications**: Ensure notifications are enabled
3. **Check Alert History**: Messages should appear in the list
4. **Check Logs**: Look for message arrival logs

## Next Steps
Once basic communication is working:
1. Test with real GPS coordinates
2. Integrate with ESP32 sensor data
3. Test emergency alert scenarios
4. Implement response acknowledgments
5. Add message encryption for security

## Support
If you continue to have issues:
1. Check the app logs for detailed error messages
2. Verify network connectivity and firewall settings
3. Try using a different MQTT broker
4. Ensure both devices are running the same app version
