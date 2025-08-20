# MQTT Communication Troubleshooting Guide

This guide addresses common issues preventing communication between Smartphone A (Crash Victim) and Smartphone B (Emergency Responder) through the local Mosquitto broker.

## 🚨 Common Issues and Solutions

### Issue 1: "MQTT broker is not accessible"

**Symptoms:**
- Both phones show "MQTT: Disconnected" status
- Test connection fails with "broker not accessible" error
- No communication between devices

**Root Causes:**
1. Mosquitto broker not running
2. Wrong IP address configured
3. Firewall blocking port 1883
4. Devices not on same network

**Solutions:**

#### Check Mosquitto Status:
```bash
# Windows
netstat -an | findstr 1883
sc query mosquitto

# Linux
sudo systemctl status mosquitto
netstat -an | grep 1883

# macOS
brew services list | grep mosquitto
lsof -i :1883
```

#### Verify Correct IP Address:
```bash
# Windows
ipconfig

# Linux/macOS
ifconfig
# or
ip addr show
```

#### Test Broker Connectivity:
```bash
# Test from laptop
mosquitto_pub -h localhost -t "test" -m "test message"

# Test from smartphone network
mosquitto_pub -h YOUR_LAPTOP_IP -t "test" -m "test message"
```

#### Fix Firewall Issues:
```bash
# Windows
netsh advfirewall firewall add rule name="MQTT" dir=in action=allow protocol=TCP localport=1883

# Linux
sudo ufw allow 1883

# macOS
# Check System Preferences > Security & Privacy > Firewall
```

### Issue 2: "Connection refused" or "Connection timeout"

**Symptoms:**
- MQTT connection attempts fail immediately
- App shows "Connection refused" error
- No network connectivity to broker

**Root Causes:**
1. Mosquitto not listening on external interfaces
2. Wrong port configuration
3. Network routing issues
4. Broker configuration problems

**Solutions:**

#### Check Mosquitto Configuration:
Create or edit `mosquitto.conf`:
```conf
# Allow external connections
listener 1883 0.0.0.0

# Allow anonymous connections (for testing)
allow_anonymous true

# Log connections for debugging
log_type all
```

#### Restart Mosquitto with New Config:
```bash
# Windows
net stop mosquitto
net start mosquitto

# Linux
sudo systemctl restart mosquitto

# macOS
brew services restart mosquitto
```

#### Test Network Connectivity:
```bash
# From smartphone, test if you can reach the laptop
ping YOUR_LAPTOP_IP

# Test specific port
telnet YOUR_LAPTOP_IP 1883
```

### Issue 3: "No messages received" between phones

**Symptoms:**
- Both phones show "MQTT: Connected" but no communication
- Test alerts not received
- Emergency alerts don't trigger notifications

**Root Causes:**
1. Topic subscription issues
2. Message format problems
3. QoS level mismatches
4. Client ID conflicts

**Solutions:**

#### Verify Topic Subscriptions:
Check that phones are subscribing to correct topics:

**Publisher (Smartphone A) subscribes to:**
- `emergency/status/system`
- `emergency/response/broadcast`
- `emergency/alerts/{incidentId}` (if specific incident)

**Subscriber (Smartphone B) subscribes to:**
- `emergency/alerts/broadcast`
- `emergency/alerts/+`
- `emergency/status/+`
- `emergency/response/+`

#### Test Topic Communication:
```bash
# Subscribe to all emergency topics
mosquitto_sub -h localhost -t "emergency/#" -v

# Publish test message
mosquitto_pub -h localhost -t "emergency/alerts/test" -m '{"type":"test","message":"Hello"}'
```

#### Check Client IDs:
Ensure each phone has unique client ID:
- Publisher: `car_crash_client_PUBLISHER_{timestamp}`
- Subscriber: `car_crash_client_SUBSCRIBER_{timestamp}`

### Issue 4: "MQTT service not enabled"

**Symptoms:**
- App shows "MQTT: Disabled" status
- Cannot send or receive messages
- MQTT service not starting

**Root Causes:**
1. MQTT service disabled in app
2. Service startup failures
3. Permission issues
4. Configuration problems

**Solutions:**

#### Enable MQTT Service:
1. Open app on both phones
2. Go to Settings → MQTT Settings
3. Click "Enable MQTT Service"
4. Wait for "MQTT Service Enabled" confirmation

#### Check Service Status:
```bash
# Check if MQTT service is running
adb shell dumpsys activity services | grep MqttService

# Check app logs
adb logcat | grep MqttService
```

#### Verify Permissions:
Ensure app has these permissions:
- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `WAKE_LOCK`

### Issue 5: "Network unavailable" or "No internet connection"

**Symptoms:**
- App shows network connectivity issues
- MQTT connection fails due to network
- Cannot reach broker

**Root Causes:**
1. WiFi connection problems
2. Network restrictions
3. DNS issues
4. Mobile data vs WiFi conflicts

**Solutions:**

#### Check Network Status:
```bash
# Check WiFi connection
adb shell dumpsys wifi | grep "mWifiInfo"

# Check network connectivity
adb shell ping YOUR_LAPTOP_IP
```

#### Verify Network Configuration:
1. Ensure all devices on same WiFi network
2. Check WiFi password and connection
3. Disable mobile data to avoid conflicts
4. Try reconnecting to WiFi

#### Test Network Connectivity:
```bash
# From smartphone, test basic connectivity
ping 8.8.8.8
ping YOUR_LAPTOP_IP
```

## 🔧 Advanced Troubleshooting

### Debug MQTT Communication:

#### Enable Verbose Logging:
```bash
# Start Mosquitto with verbose logging
mosquitto -v -c mosquitto.conf
```

#### Monitor MQTT Traffic:
```bash
# Subscribe to all topics to monitor traffic
mosquitto_sub -h localhost -t "#" -v

# Monitor specific emergency topics
mosquitto_sub -h localhost -t "emergency/#" -v
```

#### Test Message Flow:
```bash
# Terminal 1: Subscribe to emergency alerts
mosquitto_sub -h localhost -t "emergency/alerts/+" -v

# Terminal 2: Publish test alert
mosquitto_pub -h localhost -t "emergency/alerts/test123" -m '{"type":"emergency_alert","incidentId":"test123","victimName":"Test User"}'
```

### Check App Logs:

#### Enable Debug Logging:
```bash
# Enable verbose logging in app
adb shell setprop log.tag.MqttService VERBOSE
adb shell setprop log.tag.AndroidXMqttClient VERBOSE
```

#### Monitor App Logs:
```bash
# Filter MQTT-related logs
adb logcat | grep -E "(MqttService|AndroidXMqttClient|MqttConfig)"

# Monitor all app logs
adb logcat | grep "com.example.cc"
```

### Network Diagnostics:

#### Check Network Interfaces:
```bash
# Check all network interfaces
adb shell ip addr show

# Check WiFi status
adb shell dumpsys wifi
```

#### Test Port Connectivity:
```bash
# Test if port 1883 is reachable
adb shell nc -zv YOUR_LAPTOP_IP 1883

# Alternative using telnet
adb shell telnet YOUR_LAPTOP_IP 1883
```

## 📱 Phone-Specific Issues

### Android Version Compatibility:
- **Android 8+**: Ensure background service permissions
- **Android 10+**: Check battery optimization settings
- **Android 11+**: Verify network permissions

### Device-Specific Issues:
- **Samsung**: Check battery optimization for app
- **Huawei**: Verify app is not being killed by system
- **Xiaomi**: Check MIUI battery optimization settings

### App Permissions:
Ensure these permissions are granted:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

## 🎯 Quick Fix Checklist

### Before Testing:
- [ ] Mosquitto broker running on laptop
- [ ] All devices on same WiFi network
- [ ] Firewall allows port 1883
- [ ] Correct IP address configured
- [ ] MQTT service enabled on both phones

### During Testing:
- [ ] Both phones show "MQTT: Connected"
- [ ] Test connection successful
- [ ] Test alert sent and received
- [ ] Emergency alert triggers notification
- [ ] Response acknowledgment works

### If Issues Persist:
- [ ] Check Mosquitto logs for errors
- [ ] Verify network connectivity
- [ ] Test with command line tools
- [ ] Check app logs for errors
- [ ] Restart all services and devices

## 📞 Emergency Recovery

### If Nothing Works:
1. **Restart Everything**:
   ```bash
   # Restart Mosquitto
   sudo systemctl restart mosquitto  # Linux
   net stop mosquitto && net start mosquitto  # Windows
   brew services restart mosquitto  # macOS
   
   # Restart phones
   # Restart WiFi router
   ```

2. **Use Localhost Testing**:
   - Configure both phones to use `localhost` instead of IP
   - Test on same device first

3. **Check Alternative Ports**:
   - Try port 1884 or 1885
   - Update Mosquitto config and app settings

4. **Use Public Broker for Testing**:
   - Temporarily use `test.mosquitto.org:1883`
   - Verify app functionality works

## 📊 Success Indicators

### When Everything Works:
- ✅ Both phones show "MQTT: Connected"
- ✅ Test alerts sent and received immediately
- ✅ Emergency alerts trigger notifications
- ✅ Response acknowledgments work
- ✅ No connection timeouts or errors
- ✅ Real-time communication established

### Performance Metrics:
- Connection time: < 5 seconds
- Message delivery: < 1 second
- Reconnection time: < 10 seconds
- Uptime: > 99% during testing

---

**🎯 Goal**: Establish reliable, real-time MQTT communication between crash victim and emergency responder smartphones for academic demonstration and emergency response scenarios.
