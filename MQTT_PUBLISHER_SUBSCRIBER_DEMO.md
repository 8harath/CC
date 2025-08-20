# MQTT Publisher-Subscriber Communication Demo

## Overview
This guide demonstrates how to test the MQTT communication between publisher and subscriber devices in the Car Crash Detection app.

## Prerequisites
- Two Android devices (or one device and an emulator)
- Both devices connected to the same network
- MQTT broker running (local Mosquitto or public broker)

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
   - You should see a success toast: "Simple message sent to emergency/test/message"

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
- Ensure both devices are on the same network
- Check firewall settings
- Try using a different MQTT broker
- Verify internet connectivity

### Message Not Received
- Check that both devices show "MQTT: Connected"
- Verify the subscriber is in the correct mode
- Check notification permissions
- Look at the app logs for error messages

### Common Error Messages
- **"MQTT: Disconnected"**: Network or broker issues
- **"Failed to connect"**: Check broker URL and credentials
- **"Message not delivered"**: Check QoS settings and network stability

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

## Next Steps
Once basic communication is working:
1. Test with real GPS coordinates
2. Integrate with ESP32 sensor data
3. Test emergency alert scenarios
4. Implement response acknowledgments
5. Add message encryption for security
