# Local MQTT Broker Setup Guide

This guide will help you set up and run an MQTT broker locally on your system for development and testing.

## Prerequisites

- **Operating System**: Windows, macOS, or Linux
- **Network Access**: Port 1883 should be available
- **Python** (optional, for testing): Python 3.6+ with paho-mqtt

## Step 1: Install Mosquitto MQTT Broker

### Windows Installation

#### Option A: Using Chocolatey (Recommended)
```powershell
# Run PowerShell as Administrator
# Install Chocolatey first if you don't have it
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# Install Mosquitto
choco install mosquitto
```

#### Option B: Manual Installation
1. Download from: https://mosquitto.org/download/
2. Extract to `C:\mosquitto`
3. Add `C:\mosquitto` to your PATH environment variable
4. Restart your terminal/command prompt

### macOS Installation
```bash
brew install mosquitto
```

### Ubuntu/Debian Installation
```bash
sudo apt update
sudo apt install mosquitto mosquitto-clients
```

### Other Linux Distributions
```bash
# CentOS/RHEL
sudo yum install mosquitto

# Arch Linux
sudo pacman -S mosquitto
```

## Step 2: Verify Installation

Check if Mosquitto is installed correctly:
```bash
# Check version
mosquitto --version

# Check if command is available
which mosquitto  # Linux/macOS
where mosquitto  # Windows
```

## Step 3: Start the Local MQTT Broker

### Windows
```bash
# Double-click the batch file
start_mosquitto.bat

# Or run manually
mosquitto -c mosquitto_local.conf -v
```

### Linux/macOS
```bash
# Make script executable
chmod +x start_mosquitto.sh

# Run the script
./start_mosquitto.sh

# Or run manually
mosquitto -c mosquitto_local.conf -v
```

### Manual Start
```bash
# Create data directory
mkdir -p mosquitto_data

# Start broker with configuration
mosquitto -c mosquitto_local.conf -v
```

## Step 4: Test the Local Broker

### Using Python Test Script
```bash
# Install paho-mqtt if you don't have it
pip install paho-mqtt

# Run the test script
python test_local_broker.py
```

### Using Command Line Tools
```bash
# Subscribe to a topic (in one terminal)
mosquitto_sub -h localhost -t "emergency/test" -v

# Publish a message (in another terminal)
mosquitto_pub -h localhost -t "emergency/test" -m "Hello from command line!"
```

## Step 5: Test with Android App

1. **Build and install** the updated Android app
2. **Open Publisher mode** and enable MQTT
3. **Send test messages** - they should work with localhost:1883
4. **Open Subscriber mode** and enable MQTT
5. **Receive messages** from the publisher

## Configuration Details

### Broker Settings
- **Port**: 1883 (standard MQTT port)
- **Authentication**: Disabled for local development
- **Logging**: Full logging to `mosquitto_local.log`
- **Persistence**: Enabled with data stored in `mosquitto_data/`

### Topic Structure
The app uses these MQTT topics:
- `emergency/alerts/#` - Emergency alerts
- `emergency/test/#` - Test messages
- `emergency/custom/#` - Custom messages
- `emergency/response/#` - Response messages

## Troubleshooting

### Issue: "Port 1883 already in use"
```bash
# Check what's using port 1883
netstat -an | grep 1883  # Linux/macOS
netstat -an | findstr 1883  # Windows

# Kill the process if needed
sudo lsof -ti:1883 | xargs kill -9  # Linux/macOS
# For Windows, use Task Manager or Resource Monitor
```

### Issue: "Permission denied"
```bash
# Make sure you have write permissions
chmod 755 mosquitto_data
chmod 644 mosquitto_local.conf
```

### Issue: "Firewall blocking connection"
- **Windows**: Check Windows Firewall settings
- **macOS**: Check System Preferences > Security & Privacy > Firewall
- **Linux**: Check iptables/ufw settings

### Issue: "Configuration file not found"
```bash
# Make sure you're in the correct directory
ls -la mosquitto_local.conf

# Or specify full path
mosquitto -c /full/path/to/mosquitto_local.conf -v
```

## Advanced Configuration

### Enable WebSocket Support
Uncomment these lines in `mosquitto_local.conf`:
```conf
listener 9001
protocol websockets
```

### Enable SSL/TLS
```conf
listener 8883
certfile /path/to/cert.pem
keyfile /path/to/key.pem
```

### Set up Authentication
```conf
password_file mosquitto_passwd
acl_file mosquitto_acl
```

## Monitoring and Logs

### View Real-time Logs
```bash
# Follow log file
tail -f mosquitto_local.log

# Filter for specific topics
grep "emergency" mosquitto_local.log
```

### Check Broker Status
```bash
# Check if broker is running
ps aux | grep mosquitto

# Check network connections
netstat -an | grep 1883
```

## Switching Between Brokers

### Use Local Broker (Default)
```kotlin
// In your Android app
MqttConfig.setUseLocalBroker(true)
```

### Use Network Broker
```kotlin
// In your Android app
MqttConfig.setUseNetworkBroker(true)
```

### Use Custom Broker
```kotlin
// In your Android app
MqttConfig.setCustomBroker("192.168.1.100", 1883)
```

## Success Indicators

✅ **Broker Running**: `mosquitto -c mosquitto_local.conf -v` shows startup messages
✅ **Port Listening**: `netstat -an | grep 1883` shows port 1883 in LISTEN state
✅ **Test Script**: `python test_local_broker.py` completes successfully
✅ **Android App**: Connects and sends/receives messages
✅ **Logs**: `mosquitto_local.log` shows connection and message activity

## Next Steps

Once your local MQTT broker is working:
1. Test with multiple Android devices
2. Test message persistence and delivery
3. Test with different QoS levels
4. Test emergency alert scenarios
5. Test custom message functionality

## Support

If you encounter issues:
1. Check the logs in `mosquitto_local.log`
2. Verify network connectivity
3. Check firewall settings
4. Ensure port 1883 is available
5. Verify configuration file syntax
