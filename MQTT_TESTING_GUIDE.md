# MQTT Testing Guide

This guide will help you test the MQTT communication between the broker, publisher, and subscriber.

## Prerequisites

1. **MQTT Broker**: Ensure your laptop is running an MQTT broker (like Mosquitto) on IP `10.0.0.208:1883`
2. **Network**: Both devices should be on the same network
3. **Android App**: Build and install the app on your Android device

## Step 1: Test Broker Connectivity

### Option A: Using the Batch File (Windows)
```bash
test_mqtt_broker.bat
```

### Option B: Using Python Script
```bash
python test_mqtt_broker.py
```

### Option C: Manual Network Test
```bash
# Test ping
ping 10.0.0.208

# Test port (Windows PowerShell)
powershell -Command "try { $tcp = New-Object System.Net.Sockets.TcpClient; $tcp.Connect('10.0.0.208', 1883); $tcp.Close(); Write-Host 'Port 1883: OPEN' } catch { Write-Host 'Port 1883: CLOSED' }"
```

## Step 2: Test Android App Publisher

1. **Open the app** and navigate to "Crash Victim Mode" (Publisher)
2. **Enable MQTT** by clicking the "Enable MQTT" button
3. **Wait for connection** - you should see "MQTT: Connected"
4. **Test the buttons**:
   - **Test MQTT Connection**: Should show success
   - **Send Test Message**: Sends emergency alert format
   - **Send Simple Message**: Sends simple text message
   - **Send Custom Message**: Opens dialog for custom text

## Step 3: Test Android App Subscriber

1. **Open the app** and navigate to "Emergency Responder Mode" (Subscriber)
2. **Enable MQTT** by clicking the "Enable MQTT" button
3. **Wait for connection** - you should see "MQTT: Connected"
4. **Keep this screen open** to receive messages

## Step 4: Test Message Flow

### Test 1: Simple Test Message
1. In **Publisher**: Click "Send Simple Message"
2. In **Subscriber**: You should see a notification and the message in the alert history
3. **Expected Result**: Message appears in subscriber's list

### Test 2: Custom Message
1. In **Publisher**: Click "Send Custom Message" and enter any text
2. In **Subscriber**: You should see a notification and the custom message
3. **Expected Result**: Custom message appears in subscriber's list

### Test 3: Emergency Alert
1. In **Publisher**: Click "Send Test Message"
2. In **Subscriber**: You should see an emergency alert notification
3. **Expected Result**: Emergency alert appears in subscriber's list

## Troubleshooting

### Issue: "MQTT: Disconnected"
- Check if broker is running on `10.0.0.208:1883`
- Verify network connectivity
- Check firewall settings

### Issue: Messages not received
- Ensure both apps have MQTT enabled
- Check that subscriber is subscribed to correct topics
- Verify topic names match between publisher and subscriber

### Issue: Connection timeout
- Increase connection timeout in `MqttConfig.kt`
- Check broker configuration
- Verify network latency

## Topic Structure

The app uses these MQTT topics:

- **Emergency Alerts**: `emergency/alerts/{incidentId}`
- **Test Messages**: `emergency/test/message`
- **Custom Messages**: `emergency/custom/message`
- **Response Acknowledgments**: `emergency/response/ack/{incidentId}`

## Logs

Check Android Studio Logcat for detailed MQTT logs:
- Filter by tag: "MqttService"
- Look for connection status and message delivery

## Expected Behavior

1. **Publisher connects** to broker and subscribes to response topics
2. **Subscriber connects** to broker and subscribes to alert topics
3. **Messages flow** from publisher to subscriber via broker
4. **Notifications appear** on subscriber device for each message
5. **Message history** is maintained in subscriber's alert list

## Success Indicators

✅ **Publisher**: Shows "MQTT: Connected" and buttons are enabled
✅ **Subscriber**: Shows "MQTT: Connected" and receives notifications
✅ **Messages**: Appear in subscriber's alert history
✅ **Real-time**: Messages are delivered immediately

## Next Steps

Once basic MQTT communication is working:
1. Test with multiple subscriber devices
2. Test message persistence and retry mechanisms
3. Test emergency alert scenarios
4. Test response acknowledgment flow
