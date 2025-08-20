# Local MQTT Broker Setup - 192.168.0.101

## 🎯 **CRITICAL: This is your TOP PRIORITY**

Your MQTT communication is not working because the local broker at `192.168.0.101` is not properly configured or running.

## 🚀 **Immediate Setup Steps (5 minutes)**

### Step 1: Install Mosquitto on 192.168.0.101
```bash
# Windows (on the device with IP 192.168.0.101)
# Download from: https://mosquitto.org/download/
# Install the Windows installer

# After installation, start the service:
net start mosquitto
```

### Step 2: Verify Mosquitto is Running
```bash
# Check if service is running
netstat -an | findstr 1883

# Should show: TCP    0.0.0.0:1883    0.0.0.0:0    LISTENING
```

### Step 3: Test Local MQTT Communication
```bash
# Open two command prompts on 192.168.0.101

# Terminal 1 - Subscribe to test topic
mosquitto_sub -h localhost -t "test" -v

# Terminal 2 - Publish test message
mosquitto_pub -h localhost -t "test" -m "Hello World"
```

### Step 4: Check Firewall Settings
- **Windows Firewall**: Allow Mosquitto through firewall
- **Port 1883**: Must be open for incoming connections
- **Network**: Allow connections from local network

## 🔧 **Firewall Configuration**

### Windows Firewall Rules
```bash
# Allow Mosquitto through Windows Firewall
netsh advfirewall firewall add rule name="MQTT Mosquitto" dir=in action=allow protocol=TCP localport=1883

# Allow incoming connections on port 1883
netsh advfirewall firewall add rule name="MQTT Port 1883" dir=in action=allow protocol=TCP localport=1883
```

### Alternative: Disable Firewall Temporarily
```bash
# For testing only - disable firewall temporarily
netsh advfirewall set allprofiles state off

# After testing, re-enable:
netsh advfirewall set allprofiles state on
```

## 📱 **App Testing After Broker Setup**

### 1. Install Updated App
- **Uninstall** old app from both devices
- **Install** new APK on both devices
- **Clear data** if needed

### 2. Test Publisher (Phone 1)
- Open **Emergency Alert Publisher**
- App will **automatically** enable MQTT
- Wait for **"MQTT: Connected"** ✅
- **"Send Simple Message"** button should be enabled

### 3. Test Subscriber (Phone 2)
- Open **Emergency Responder**
- App will **automatically** enable MQTT
- Wait for **"MQTT: Connected"** ✅
- **"Test Connection"** button should be enabled

### 4. Test Communication
- **Publisher**: Tap "Send Simple Message"
- **Subscriber**: Should receive notification immediately
- **Alert History**: Message should appear in the list

## 🚨 **Troubleshooting**

### If Still Not Working:

#### Check Network Connectivity
```bash
# From both phones, test connectivity to broker
ping 192.168.0.101

# Should get response from 192.168.0.101
```

#### Check Mosquitto Status
```bash
# On 192.168.0.101 device
netstat -an | findstr 1883
# Should show: TCP    0.0.0.0:1883    0.0.0.0:0    LISTENING

# Check Mosquitto service
sc query mosquitto
# Should show: RUNNING
```

#### Test MQTT Port
```bash
# From another device on the network
telnet 192.168.0.101 1883
# Should connect (press Ctrl+C to exit)
```

#### Check App Logs
```bash
# On both phones, check MQTT logs
adb logcat | grep -i mqtt

# Look for:
# - Connection attempts
# - Connection success/failure
# - Message publishing
# - Message receiving
```

## ✅ **Success Indicators**

- ✅ Mosquitto service running on 192.168.0.101
- ✅ Port 1883 accessible from network
- ✅ Both phones show "MQTT: Connected"
- ✅ Publisher can send messages
- ✅ Subscriber receives notifications
- ✅ Messages appear in alert history

## 🆘 **Emergency Fixes**

### If Nothing Works:
1. **Restart Mosquitto service** on 192.168.0.101
2. **Restart both phones**
3. **Clear app data** on both phones
4. **Reinstall app** on both phones
5. **Check network settings** - ensure same WiFi

### Alternative: Use Public Broker
If local broker still doesn't work:
1. Change broker URL to: `tcp://broker.hivemq.com:1883`
2. This requires internet but no local setup

## 📞 **Support**

### Run Diagnostic Script
```bash
# Windows
test_mqtt_local_broker.bat
```

### Check Logs
```bash
# MQTT Service logs
adb logcat | grep -i MqttService

# Publisher logs
adb logcat | grep -i PublisherActivity

# Subscriber logs
adb logcat | grep -i SubscriberActivity
```

---

## 🎯 **SUMMARY**

**The issue is NOT with the app code** - it's with the local MQTT broker setup.

**To fix:**
1. ✅ Install and start Mosquitto on 192.168.0.101
2. ✅ Open port 1883 in firewall
3. ✅ Install updated app on both phones
4. ✅ Test communication

**Once Mosquitto is running on 192.168.0.101, the communication will work immediately!**
