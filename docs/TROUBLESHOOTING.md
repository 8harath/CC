# Troubleshooting Guide

## Common Issues and Solutions

### MQTT Connection Issues

#### Connection Fails
**Symptoms**: App shows "MQTT Connection Failed" or similar error
**Solutions**:
1. **Verify Broker Status**
   ```bash
   # Windows
   check_mosquitto.bat
   
   # Linux/Mac
   ./check_mosquitto.sh
   ```

2. **Check Network Connectivity**
   - Ensure device and broker are on same network
   - Test with `ping <broker-ip>`
   - Verify firewall settings

3. **Verify Broker Configuration**
   - Check `MqttConfig.kt` for correct broker URL
   - Ensure port 1883 is open
   - Verify broker accepts anonymous connections

#### Intermittent Disconnections
**Symptoms**: Connection drops randomly
**Solutions**:
1. **Enable Auto-Reconnection**
   - Check MQTT client settings
   - Verify keep-alive interval (60 seconds recommended)

2. **Network Stability**
   - Check WiFi signal strength
   - Consider using wired connection for broker
   - Monitor network interference

### ESP32 Integration Issues

#### Bluetooth Not Connecting
**Symptoms**: ESP32 not found or connection fails
**Solutions**:
1. **Verify ESP32 Setup**
   ```bash
   test_bluetooth_setup.bat
   ```

2. **Check Permissions**
   - Ensure Bluetooth permissions granted
   - Enable location services (required for BLE)

3. **ESP32 Firmware**
   - Verify correct firmware uploaded
   - Check Serial Monitor for ESP32 status
   - Ensure MPU6050 properly connected

#### Sensor Data Issues
**Symptoms**: No sensor readings or incorrect data
**Solutions**:
1. **Hardware Connections**
   - Verify MPU6050 wiring
   - Check power supply (3.3V)
   - Ensure proper I2C connections

2. **Calibration**
   - Run sensor calibration routine
   - Check for magnetic interference
   - Verify sensor orientation

### App Performance Issues

#### Slow Startup
**Symptoms**: App takes long time to launch
**Solutions**:
1. **Database Optimization**
   - Check database size
   - Clear unnecessary data
   - Optimize queries

2. **Memory Management**
   - Monitor memory usage
   - Close background apps
   - Restart device if needed

#### UI Responsiveness
**Symptoms**: App feels sluggish or unresponsive
**Solutions**:
1. **Background Processes**
   - Check for heavy background tasks
   - Optimize MQTT message processing
   - Reduce UI update frequency

2. **Device Performance**
   - Ensure sufficient RAM available
   - Check CPU usage
   - Update Android version if possible

### Build and Development Issues

#### Gradle Sync Fails
**Symptoms**: Android Studio shows sync errors
**Solutions**:
1. **Network Issues**
   - Check internet connection
   - Use VPN if needed
   - Clear Gradle cache

2. **Dependency Issues**
   - Update Gradle version
   - Check dependency versions
   - Invalidate caches and restart

#### Build Errors
**Symptoms**: Compilation fails
**Solutions**:
1. **Java/Kotlin Issues**
   - Verify JDK 11+ installed
   - Set JAVA_HOME correctly
   - Update Android Studio

2. **Code Issues**
   - Check for syntax errors
   - Verify imports
   - Clean and rebuild project

### Database Issues

#### Data Loss
**Symptoms**: App data disappears after restart
**Solutions**:
1. **Database Migration**
   - Check for schema changes
   - Verify migration scripts
   - Backup data before updates

2. **Storage Issues**
   - Check device storage space
   - Verify database file permissions
   - Clear app data if corrupted

### Testing and Debugging

#### MQTT Testing
```bash
# Test broker connectivity
test_mqtt_broker.bat

# Test local broker
test_mqtt_local_broker.bat

# Test communication
diagnose_mqtt_communication.bat
```

#### Python Test Scripts
```bash
# Test broker functionality
python test_mqtt_broker.py

# Test local broker
python test_local_broker.py

# Test network validation
python test_ip_validation_and_messaging.py
```

### Debug Tools

#### Android Studio
1. **Logcat**: View detailed logs
   - Filter by app package
   - Search for error keywords
   - Monitor MQTT messages

2. **Database Inspector**: View Room database
   - Located in View → Tool Windows → App Inspection
   - Monitor data changes
   - Verify CRUD operations

3. **Layout Inspector**: Debug UI issues
   - Inspect view hierarchy
   - Check layout performance
   - Verify view states

#### Built-in Debug Features
1. **MQTT Test Activity**: Test MQTT functionality
2. **System Health Monitor**: Monitor app performance
3. **Production Dashboard**: View system status

### Emergency Recovery

#### App Won't Launch
1. **Clear App Data**
   - Settings → Apps → Car Crash Detection → Storage → Clear Data
   - Reinstall app if necessary

2. **Check Permissions**
   - Verify all required permissions granted
   - Re-grant permissions if needed

#### Complete Reset
1. **Uninstall App**
2. **Clear Device Storage**
3. **Reinstall Fresh Copy**
4. **Reconfigure Settings**

### Performance Optimization

#### Memory Management
- Monitor memory usage in Android Studio
- Optimize image loading and caching
- Reduce background service usage

#### Battery Optimization
- Disable unnecessary background processes
- Optimize MQTT keep-alive intervals
- Use efficient data structures

#### Network Optimization
- Compress MQTT messages
- Implement message queuing
- Use efficient serialization formats

### Support Resources

1. **Documentation**: Check relevant guides in `/docs` folder
2. **Logs**: Review Android Studio logcat for detailed errors
3. **Test Scripts**: Use provided test scripts for validation
4. **Community**: Check project issues and discussions

---

**Note**: Always test solutions in a development environment before applying to production.
