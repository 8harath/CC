# MQTT Communication Troubleshooting Guide

## Problem Analysis: Publisher → Broker → Subscriber Pipeline

Based on your description, you're experiencing a classic MQTT communication issue where:
- ✅ Publisher successfully connects and publishes messages
- ✅ Broker receives and logs messages (visible in Mosquitto logs)
- ❌ Subscriber doesn't receive messages despite being connected

This indicates the issue is in the **broker → subscriber** leg of the pipeline.

## Root Cause Analysis

### 1. **Topic Subscription Mismatch** (Most Likely Cause)
**Problem**: Publisher and subscriber are using different topic patterns.

**Publisher publishes to**: `emergency/alerts/broadcast`, `emergency/custom/message`, etc.
**Subscriber subscribes to**: `emergency/alerts/#` (should work, but let's verify)

**Solution**: Enhanced topic subscription in the subscriber:
```kotlin
// Subscribe to ALL possible emergency topics
val allEmergencyTopics = listOf(
    "emergency/alerts/#",           // Emergency alerts
    "emergency/test/#",             // Test messages  
    "emergency/custom/#",           // Custom messages
    "emergency/response/#",         // Response messages
    "emergency/status/#",           // Status messages
    "emergency/#"                   // Catch-all for any emergency topic
)
```

### 2. **Client ID Conflicts**
**Problem**: Multiple clients with same client ID can cause subscription issues.

**Solution**: Ensure unique client IDs:
```kotlin
// In MqttConfig.kt
fun getClientId(): String {
    val savedId = prefs?.getString(KEY_CLIENT_ID, null)
    return if (savedId != null) {
        savedId
    } else {
        val newId = "android_client_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
        prefs?.edit()?.putString(KEY_CLIENT_ID, newId)?.apply()
        newId
    }
}
```

### 3. **QoS Level Mismatches**
**Problem**: Publisher uses QoS 1, subscriber expects QoS 0 (or vice versa).

**Solution**: Ensure consistent QoS levels:
```kotlin
// Both publisher and subscriber should use same QoS
mqttClient.subscribe(topic, 1, null, actionListener)  // QoS 1
mqttClient.publish(topic, message, 1, false)         // QoS 1
```

### 4. **Connection Timing Issues**
**Problem**: Subscriber subscribes before fully connected.

**Solution**: Subscribe after connection is confirmed:
```kotlin
mqttClient.connect(options, null, object : IMqttActionListener {
    override fun onSuccess(asyncActionToken: IMqttToken?) {
        // Only subscribe after successful connection
        subscribeForRole(role, incidentId)
    }
})
```

## Step-by-Step Diagnostic Process

### Step 1: Run the Diagnostic Script
```bash
# Install required package
pip install paho-mqtt

# Run diagnostic on your broker
python scripts/diagnose_mqtt_communication.py 192.168.1.100 1883
```

### Step 2: Verify Broker Configuration
```bash
# Check if Mosquitto is running
mosquitto -v -p 1883

# Test with command line tools
mosquitto_pub -h 192.168.1.100 -t "emergency/test" -m "test message"
mosquitto_sub -h 192.168.1.100 -t "emergency/#"
```

### Step 3: Check Android App Logs
Enable verbose logging in your Android app:
```kotlin
// In MqttService.kt
private const val TAG = "MqttService"
Log.d(TAG, "🔍 Verbose logging enabled")

// Add detailed connection logging
override fun onConnectComplete(reconnect: Boolean, serverURI: String?) {
    Log.i(TAG, "Connection complete - reconnect: $reconnect, URI: $serverURI")
}
```

### Step 4: Verify Topic Subscriptions
Check that subscriber is actually subscribing to the right topics:
```kotlin
// Add subscription confirmation logging
mqttClient.subscribe(topic, 1, null, object : IMqttActionListener {
    override fun onSuccess(asyncActionToken: IMqttToken?) {
        Log.i(TAG, "✅ Successfully subscribed to: $topic")
    }
    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
        Log.e(TAG, "❌ Failed to subscribe to $topic: ${exception?.message}")
    }
})
```

## Immediate Fixes to Apply

### Fix 1: Enhanced Topic Subscription
The subscriber now subscribes to ALL emergency topics to ensure no messages are missed.

### Fix 2: Improved Connection Verification
```kotlin
private fun verifyConnection() {
    // Test publishing and subscribing to same topic
    val testTopic = "emergency/test/connection"
    val testPayload = "Connection test - ${System.currentTimeMillis()}"
    
    // Publish test message
    mqttClient.publish(testTopic, testPayload, 1, false)
    
    // Subscribe to same topic
    mqttClient.subscribe(testTopic, 1)
    
    // Send verification message
    mqttClient.publish(testTopic, "Verification message", 1, false)
}
```

### Fix 3: Better Error Handling
```kotlin
override fun messageArrived(topic: String?, message: MqttMessage?) {
    Log.i(TAG, "📨 Message arrived: $topic -> ${String(message?.payload ?: ByteArray(0))}")
    
    // Handle all message types
    when {
        topic?.startsWith("emergency/alerts/") == true -> {
            // Handle emergency alerts
            handleEmergencyAlert(topic, message)
        }
        topic?.startsWith("emergency/test/") == true -> {
            // Handle test messages
            handleTestMessage(topic, message)
        }
        topic?.startsWith("emergency/custom/") == true -> {
            // Handle custom messages
            handleCustomMessage(topic, message)
        }
        else -> {
            // Handle any other emergency message
            handleGeneralMessage(topic, message)
        }
    }
}
```

## Testing Protocol

### Test 1: Basic Connectivity
1. Start Mosquitto broker: `mosquitto -v -p 1883`
2. Run diagnostic script: `python scripts/diagnose_mqtt_communication.py <broker_ip>`
3. Verify all tests pass

### Test 2: Command Line End-to-End
```bash
# Terminal 1: Subscribe
mosquitto_sub -h 192.168.1.100 -t "emergency/#" -v

# Terminal 2: Publish
mosquitto_pub -h 192.168.1.100 -t "emergency/test/message" -m "Hello World"
```

### Test 3: Android App Testing
1. **Publisher App**:
   - Connect to broker
   - Send test message: "hi", "test", or any simple text
   - Verify "emergency alert sent successfully" notification

2. **Subscriber App**:
   - Connect to same broker
   - Wait for connection confirmation
   - Check if message appears in alerts list

### Test 4: Topic Verification
Use the diagnostic script to verify specific topics:
```bash
python scripts/diagnose_mqtt_communication.py 192.168.1.100 1883
```

## Expected Message Delivery Delays

### Normal Delays (Expected)
- **Network latency**: 10-100ms on local network
- **MQTT handshake**: 50-200ms
- **Message processing**: 10-50ms
- **Total expected delay**: 70-350ms

### Abnormal Delays (Problematic)
- **Connection timeouts**: >5 seconds
- **Message delivery delays**: >1 second
- **Intermittent delivery**: Messages arrive sometimes but not always

## Debugging Checklist

### Before Testing
- [ ] Mosquitto broker is running and accessible
- [ ] Both phones are on same network
- [ ] Broker IP address is correct in both apps
- [ ] No firewall blocking port 1883

### During Testing
- [ ] Publisher shows "connected" status
- [ ] Subscriber shows "connected" status
- [ ] Publisher shows "message sent" notification
- [ ] Subscriber receives message notification
- [ ] Check Android logs for detailed error messages

### After Testing
- [ ] Review diagnostic report
- [ ] Check Mosquitto logs for errors
- [ ] Verify topic subscriptions in Android logs
- [ ] Test with command line tools

## Common Error Messages and Solutions

### "Connection refused"
**Cause**: Broker not running or wrong port
**Solution**: Start Mosquitto with `mosquitto -p 1883`

### "Connection timeout"
**Cause**: Network connectivity issue
**Solution**: Check IP address and network connection

### "Client ID already in use"
**Cause**: Multiple clients with same ID
**Solution**: Ensure unique client IDs

### "Subscription failed"
**Cause**: Topic format issue or connection problem
**Solution**: Check topic format and connection status

### "Message not received"
**Cause**: Topic subscription mismatch
**Solution**: Verify subscriber subscribes to correct topics

## Minimal Working Example

### Test Message Format
```json
{
  "topic": "emergency/test/simple",
  "payload": "Hello World",
  "qos": 1,
  "retained": false
}
```

### Expected Flow
1. Publisher connects to broker
2. Publisher publishes to `emergency/test/simple`
3. Broker receives and processes message
4. Subscriber receives message on `emergency/test/simple`
5. Subscriber displays notification

### Verification Commands
```bash
# Test with command line tools
mosquitto_pub -h 192.168.1.100 -t "emergency/test/simple" -m "Hello World"
mosquitto_sub -h 192.168.1.100 -t "emergency/test/simple"
```

## Next Steps

1. **Run the diagnostic script** to identify the exact issue
2. **Apply the topic subscription fixes** in the code
3. **Test with command line tools** to verify broker functionality
4. **Test with Android apps** using simple messages
5. **Check logs** for detailed error information

The enhanced topic subscription should resolve the most common cause of this issue. If problems persist, the diagnostic script will provide specific guidance based on your setup.
