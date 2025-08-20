# Local MQTT Setup Guide for Smartphone Communication

This guide will help you set up MQTT communication between two smartphones (Crash Victim and Emergency Responder) using a local Mosquitto broker running on your laptop.

## 🎯 Overview

**Smartphone A (Crash Victim)**: Publishes emergency alerts when a crash is detected  
**Smartphone B (Emergency Responder)**: Subscribes to emergency alerts and responds  
**Laptop**: Runs Mosquitto MQTT broker to facilitate communication

## 📋 Prerequisites

1. **Laptop with Mosquitto MQTT Broker** installed and running
2. **Two Android smartphones** with the Car Crash Detection App installed
3. **Same WiFi network** for all devices
4. **Network connectivity** between devices

## 🔧 Step 1: Install and Configure Mosquitto Broker

### Windows Installation:
```cmd
# Download and install Mosquitto from https://mosquitto.org/download/
# Start the service
net start mosquitto

# Check if it's running
netstat -an | findstr 1883
```

### Linux Installation:
```bash
sudo apt update
sudo apt install mosquitto mosquitto-clients
sudo systemctl enable mosquitto
sudo systemctl start mosquitto

# Check status
sudo systemctl status mosquitto
```

### macOS Installation:
```bash
brew install mosquitto
brew services start mosquitto

# Check status
brew services list | grep mosquitto
```

## 🌐 Step 2: Configure Network Settings

### Find Your Laptop's IP Address:

**Windows:**
```cmd
ipconfig
# Look for "IPv4 Address" under your WiFi adapter (usually 192.168.x.x)
```

**Linux/macOS:**
```bash
ifconfig
# Look for "inet" followed by your IP address (usually 192.168.x.x)
```

### Configure Firewall:
Ensure port 1883 is open on your laptop:

**Windows:**
```cmd
# Open Windows Defender Firewall
# Add inbound rule for port 1883 (TCP)
netsh advfirewall firewall add rule name="MQTT" dir=in action=allow protocol=TCP localport=1883
```

**Linux:**
```bash
sudo ufw allow 1883
```

**macOS:**
```bash
# macOS firewall should allow local connections by default
```

## 📱 Step 3: Configure Smartphone A (Crash Victim)

1. **Install the App**: Install the Car Crash Detection App on Smartphone A
2. **Open MQTT Settings**:
   - Launch the app
   - Go to Settings → MQTT Settings
   - Enter your laptop's IP address (e.g., `192.168.1.100`)
   - Set port to `1883`
   - Click "Test Connection" to verify connectivity
   - Click "Save Settings"

3. **Enable MQTT Service**:
   - Click "Enable MQTT Service"
   - Wait for "MQTT Service Enabled" confirmation

4. **Set Role as Publisher**:
   - Go back to main screen
   - Select "Publisher" mode
   - This phone will now act as the crash victim

## 📱 Step 4: Configure Smartphone B (Emergency Responder)

1. **Install the App**: Install the Car Crash Detection App on Smartphone B
2. **Open MQTT Settings**:
   - Launch the app
   - Go to Settings → MQTT Settings
   - Enter the SAME laptop IP address as Smartphone A
   - Set port to `1883`
   - Click "Test Connection" to verify connectivity
   - Click "Save Settings"

3. **Enable MQTT Service**:
   - Click "Enable MQTT Service"
   - Wait for "MQTT Service Enabled" confirmation

4. **Set Role as Subscriber**:
   - Go back to main screen
   - Select "Subscriber" mode
   - This phone will now act as the emergency responder

## 🧪 Step 5: Test Communication

### Test 1: Basic Connectivity
1. **On Smartphone A (Publisher)**:
   - Go to "Publisher" mode
   - Click "Test MQTT Connection"
   - Should show "✅ MQTT broker is accessible!"

2. **On Smartphone B (Subscriber)**:
   - Go to "Subscriber" mode
   - Click "Test MQTT Connection"
   - Should show "✅ MQTT broker is accessible!"

### Test 2: Send Test Alert
1. **On Smartphone A (Publisher)**:
   - Go to "Publisher" mode
   - Click "Send Test Emergency Alert"
   - Should show "✅ Test emergency alert sent!"

2. **On Smartphone B (Subscriber)**:
   - Go to "Subscriber" mode
   - Should receive the test alert in the alert history
   - Alert should appear with victim information

### Test 3: Real Emergency Scenario
1. **On Smartphone A (Publisher)**:
   - Go to "Publisher" mode
   - Click "Send Emergency Alert" (or simulate crash detection)
   - Fill in emergency details
   - Send the alert

2. **On Smartphone B (Subscriber)**:
   - Should receive immediate notification
   - Alert should appear in incident list
   - Can view incident details and respond

## 🔍 Troubleshooting

### Issue 1: "MQTT broker is not accessible"
**Solutions:**
- Verify Mosquitto is running: `netstat -an | findstr 1883`
- Check firewall settings
- Ensure all devices are on same WiFi network
- Try using `localhost` instead of IP address if testing on same device

### Issue 2: "Connection refused"
**Solutions:**
- Restart Mosquitto service
- Check if port 1883 is already in use
- Verify IP address is correct
- Check network connectivity

### Issue 3: "No messages received"
**Solutions:**
- Verify both phones have MQTT service enabled
- Check that roles are set correctly (Publisher/Subscriber)
- Ensure topics are properly configured
- Check app logs for error messages

### Issue 4: "Network unavailable"
**Solutions:**
- Check WiFi connection on both phones
- Ensure laptop is connected to same network
- Try reconnecting to WiFi
- Check if network has restrictions

## 📊 Monitoring and Debugging

### Check Mosquitto Logs:
```bash
# Windows
# Check Event Viewer for Mosquitto logs

# Linux
sudo journalctl -u mosquitto -f

# macOS
tail -f /usr/local/var/log/mosquitto.log
```

### Test with Command Line Tools:
```bash
# Subscribe to all emergency topics
mosquitto_sub -h localhost -t "emergency/#" -v

# Publish test message
mosquitto_pub -h localhost -t "emergency/alerts/test" -m "Test message"
```

### App Debug Information:
- Use the "MQTT Test" activity in the app
- Check logcat for detailed error messages
- Use "Check Network Status" to verify connectivity

## 🔧 Advanced Configuration

### Enable MQTT Authentication (Optional):
1. **Create password file**:
   ```bash
   mosquitto_passwd -c /etc/mosquitto/passwd android_user
   ```

2. **Update mosquitto.conf**:
   ```conf
   allow_anonymous false
   password_file /etc/mosquitto/passwd
   ```

3. **Update app settings** with username/password

### Enable SSL/TLS (Optional):
1. **Generate certificates**
2. **Configure mosquitto.conf for SSL**
3. **Update app to use port 8883**

## 📱 App Features for Testing

### Publisher Mode (Smartphone A):
- ✅ Send emergency alerts
- ✅ Include GPS location
- ✅ Add medical information
- ✅ Test MQTT connection
- ✅ View connection status

### Subscriber Mode (Smartphone B):
- ✅ Receive emergency alerts
- ✅ View incident details
- ✅ Send response acknowledgments
- ✅ Alert history
- ✅ Connection monitoring

## 🎯 Expected Behavior

### When Communication Works:
1. **Publisher sends alert** → **Subscriber receives immediately**
2. **Subscriber responds** → **Publisher receives acknowledgment**
3. **Real-time updates** on both devices
4. **Connection status** shows "Connected" on both phones
5. **Alert history** populated with all communications

### Success Indicators:
- ✅ Both phones show "MQTT: Connected" status
- ✅ Test alerts are received immediately
- ✅ Emergency alerts trigger notifications
- ✅ Response acknowledgments work
- ✅ No connection timeouts or errors

## 🚀 Next Steps

Once basic communication is working:
1. **Test with real crash scenarios**
2. **Add multiple responders**
3. **Test network interruption recovery**
4. **Implement advanced features**
5. **Deploy in production environment**

## 📞 Support

If you encounter issues:
1. Check this troubleshooting guide
2. Verify all prerequisites are met
3. Test with command line tools first
4. Check app logs and Mosquitto logs
5. Ensure network connectivity is stable

---

**🎯 Goal**: Establish reliable MQTT communication between crash victim and emergency responder smartphones through local Mosquitto broker for academic demonstration and testing.
