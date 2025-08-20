# MQTT Settings Guide

This guide explains how to use the new MQTT Settings feature that allows you to configure the MQTT broker IP address directly in the app.

## Overview

The app now includes a **MQTT Settings** feature that allows you to:
- Configure the MQTT broker IP address and port
- Test the connection to your broker
- Save settings for future use
- Use quick selection buttons for common configurations

## How to Access MQTT Settings

### From Publisher Mode:
1. Open the app and select **"Emergency Alert Publisher"**
2. Scroll down to the **"MQTT Testing"** section
3. Tap the **"MQTT Settings"** button

### From Subscriber Mode:
1. Open the app and select **"Emergency Response Dashboard"**
2. Scroll down to the **"MQTT Testing"** section
3. Tap the **"MQTT Settings"** button

## Using the MQTT Settings Screen

### 1. Broker Configuration

**IP Address Field:**
- Enter the IP address of your MQTT broker
- Examples: `192.168.1.100`, `localhost`, `10.0.0.5`

**Port Field:**
- Enter the port number (default: 1883)
- Common ports: 1883 (MQTT), 8883 (MQTT over SSL)

### 2. Quick Selection Buttons

**localhost Button:**
- Quickly sets the IP to `localhost`
- Use when testing on the same device as the broker

**192.168.1.100 Button:**
- Quickly sets the IP to `192.168.1.100`
- Common local network IP address

**Enter Custom IP Button:**
- Opens a dialog to enter any custom IP address
- Useful for specific network configurations

### 3. Connection Testing

**Test Connection Button:**
- Tests if the app can reach the specified broker
- Shows connection status (✅ Success or ❌ Failed)
- Validates both IP resolution and port connectivity

### 4. Saving Settings

**Save Settings Button:**
- Saves the current configuration
- Settings are stored locally and persist between app sessions
- Updates the MQTT configuration for all app features

## Common Configuration Examples

### Local Testing (Same Device)
```
IP Address: localhost
Port: 1883
```

### Local Network Testing
```
IP Address: 192.168.1.100
Port: 1883
```

### Custom Network
```
IP Address: 10.0.0.5
Port: 1883
```

### Cloud Broker (Example)
```
IP Address: broker.hivemq.com
Port: 1883
```

## Step-by-Step Setup

### Step 1: Configure Broker
1. Open MQTT Settings
2. Enter your broker's IP address
3. Enter the port number (usually 1883)
4. Use quick selection buttons if applicable

### Step 2: Test Connection
1. Tap "Test Connection"
2. Wait for the test to complete
3. Check the status message:
   - ✅ **Success**: Broker is reachable
   - ❌ **Failed**: Check IP/port or broker status

### Step 3: Save Settings
1. Tap "Save Settings"
2. Confirm the settings are saved
3. Return to the main screen

### Step 4: Enable MQTT
1. Go back to Publisher or Subscriber mode
2. Tap "Enable MQTT" or "Enable MQTT"
3. The app will use your saved broker settings

## Troubleshooting

### Connection Test Fails

**Possible Issues:**
1. **Wrong IP Address**: Double-check the broker IP
2. **Wrong Port**: Verify the broker port (usually 1883)
3. **Broker Not Running**: Start your Mosquitto broker
4. **Network Issues**: Check WiFi connectivity
5. **Firewall**: Ensure port 1883 is not blocked

**Solutions:**
- Verify broker is running: `mosquitto_sub -h localhost -t test -v`
- Check network connectivity: `ping [broker-ip]`
- Try different IP addresses (localhost, 127.0.0.1, etc.)

### Settings Not Saving

**Possible Issues:**
1. Invalid IP format
2. Invalid port number
3. App permissions

**Solutions:**
- Ensure IP is in correct format (e.g., 192.168.1.100)
- Port must be between 1-65535
- Check app storage permissions

### MQTT Still Not Working After Settings

**Possible Issues:**
1. Settings not applied to running service
2. Broker authentication required
3. Network configuration issues

**Solutions:**
- Restart the MQTT service in the app
- Check if broker requires username/password
- Verify network configuration

## Advanced Configuration

### Using Different Ports
- **1883**: Standard MQTT port
- **8883**: MQTT over SSL/TLS
- **9001**: WebSocket MQTT

### Network Considerations
- **Same Device**: Use `localhost` or `127.0.0.1`
- **Same Network**: Use local IP (e.g., `192.168.1.100`)
- **Different Network**: Use public IP or domain name

### Security Notes
- Default configuration uses no authentication
- For production, consider using SSL/TLS
- Some brokers require username/password

## Integration with Testing

Once MQTT settings are configured:

1. **Publisher Mode**: Can send test messages to your broker
2. **Subscriber Mode**: Can receive messages from your broker
3. **Real-time Communication**: Both modes work with your configured broker

## Success Indicators

Your MQTT settings are working correctly when:

1. ✅ **Connection Test**: Shows "Connection successful"
2. ✅ **Settings Save**: Shows "Settings saved successfully"
3. ✅ **MQTT Enable**: Status shows "MQTT: Connected"
4. ✅ **Message Testing**: Test messages are sent/received successfully

## Support

If you continue to have issues:

1. Check the app logs for detailed error messages
2. Verify your Mosquitto broker configuration
3. Test with command-line MQTT tools first
4. Ensure all network connectivity is working
5. Check app permissions for network access

## Next Steps

After configuring MQTT settings:

1. **Test Basic Communication**: Send/receive test messages
2. **Test Emergency Alerts**: Verify emergency alert flow
3. **Test Multiple Devices**: Connect multiple phones
4. **Scale Testing**: Test with more complex scenarios
