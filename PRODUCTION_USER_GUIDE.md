# Car Crash Detection App - Production User Guide

## 📖 Overview

This guide provides comprehensive instructions for deploying, monitoring, and maintaining the Car Crash Detection App in production environments. The app is designed for academic demonstrations and emergency response communication using MQTT protocol.

---

## 🚀 Quick Start Guide

### Prerequisites
- Android device running Android 7.0 (API 24) or higher
- Local MQTT broker (e.g., Mosquitto) running on network
- ESP32 crash detection hardware (optional for full functionality)
- Network connectivity to MQTT broker

### Installation Steps
1. **Install APK**: Install the production APK on your Android device
2. **First Launch**: App will guide you through initial setup
3. **Role Selection**: Choose between Publisher (victim) or Subscriber (responder) mode
4. **Configuration**: Enter MQTT broker details and other settings
5. **Demo Mode**: Enable demo mode for academic demonstrations

---

## 🏗️ Production Deployment

### Building Production APK

#### Windows
```batch
# Run the production build script
build_production.bat
```

#### Linux/Mac
```bash
# Make script executable and run
chmod +x build_production.sh
./build_production.sh
```

#### Manual Build
```bash
# Clean and build release APK
./gradlew clean
./gradlew assembleRelease
```

### Production Configuration

#### Keystore Setup
1. Create `keystore.properties` file in project root:
```properties
storeFile=../keystore/car-crash-detection.keystore
storePassword=your_keystore_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

2. Generate keystore (if not exists):
```bash
keytool -genkey -v -keystore car-crash-detection.keystore \
  -alias car-crash-detection-key \
  -keyalg RSA -keysize 2048 -validity 10000
```

#### Build Variants
- **Debug**: Development features, logging enabled, debug tools
- **Release**: Production optimized, logging disabled, ProGuard enabled

---

## 📊 Production Monitoring

### Accessing the Production Dashboard

1. **Navigate to Production Dashboard**: Access via app menu or direct navigation
2. **Start Monitoring**: Click "Start Monitoring" to begin system monitoring
3. **View System Status**: Monitor real-time system health and performance
4. **Export Reports**: Generate comprehensive system status reports

### Monitoring Features

#### System Health Monitoring
- **Memory Usage**: Real-time memory consumption and availability
- **Storage Status**: Storage space usage and availability
- **Network Status**: Connection quality and network performance
- **Database Health**: Database performance and integrity
- **ESP32 Status**: Hardware connectivity and communication health

#### Performance Metrics
- **App Startup Time**: Application launch performance
- **UI Responsiveness**: Interface response time measurements
- **Database Performance**: Query execution time and efficiency
- **MQTT Performance**: Message throughput and latency
- **GPS Performance**: Location service accuracy and speed

#### Usage Analytics
- **Feature Usage**: Track which app features are most used
- **User Interactions**: Monitor user engagement patterns
- **Error Rates**: Track system errors and failure rates
- **Performance Trends**: Historical performance data analysis

---

## 🔧 System Maintenance

### Scheduled Maintenance

#### Automatic Maintenance
- **Frequency**: Weekly (configurable)
- **Operations**: Database cleanup, file system optimization, performance tuning
- **Schedule**: Configure via maintenance settings

#### Manual Maintenance
1. **Access Production Dashboard**
2. **Click "Scheduled Maintenance"**
3. **Monitor Progress**: View real-time maintenance status
4. **Review Results**: Check maintenance completion and results

### Emergency Maintenance

#### When to Use
- System performance degradation
- Critical errors or crashes
- Data corruption issues
- Hardware connectivity problems

#### Emergency Procedures
1. **Access Production Dashboard**
2. **Click "Emergency Maintenance"**
3. **Monitor Recovery**: Watch emergency maintenance progress
4. **Verify System**: Confirm system restoration and health

### System Diagnostics

#### Running Diagnostics
1. **Click "Run System Diagnostics"**
2. **Wait for Completion**: Diagnostics run comprehensive system checks
3. **Review Results**: Analyze diagnostic findings and recommendations
4. **Address Issues**: Follow recommendations for system optimization

#### Diagnostic Components
- **System Health**: Overall system status and component health
- **Performance Analysis**: Performance metrics and bottlenecks
- **Storage Analysis**: Storage usage and optimization opportunities
- **Network Diagnostics**: Network connectivity and performance

---

## 💾 Backup and Restore

### Creating System Backups

#### Automatic Backup
- **Frequency**: Before major maintenance operations
- **Storage**: Local device storage in `backups/` directory
- **Format**: Compressed ZIP files with timestamp

#### Manual Backup
1. **Click "Create Backup"**
2. **Wait for Completion**: Backup process includes all system data
3. **Verify Backup**: Check backup file size and location
4. **Store Securely**: Move backup to secure storage location

### Restoring from Backup

#### Restore Process
1. **Select Backup File**: Choose backup file from storage
2. **Click "Restore Backup"**
3. **Confirm Operation**: Acknowledge data overwrite warning
4. **Monitor Progress**: Watch restore operation progress
5. **Verify Restoration**: Confirm system data restoration

#### Backup Contents
- **Database**: All incident and profile data
- **Configuration**: App settings and preferences
- **Profiles**: Medical profile information
- **Logs**: System operation logs and history

---

## ⚙️ Installation and Configuration

### Initial System Installation

#### First-Time Setup
1. **Launch App**: Start the application
2. **Installation Wizard**: Follow guided installation process
3. **System Initialization**: App creates necessary directories and databases
4. **Default Configuration**: Standard settings are applied
5. **Demo Data**: Sample profiles and scenarios are created

#### Installation Validation
1. **Click "Validate System"**
2. **Review Results**: Check all validation components
3. **Address Issues**: Fix any validation failures
4. **Confirm Installation**: Verify successful system setup

### Configuration Management

#### Installing Configuration Packages
1. **Prepare Config Package**: Create ZIP file with configuration data
2. **Click "Install Config"**
3. **Select Package**: Choose configuration ZIP file
4. **Monitor Installation**: Watch configuration deployment progress
5. **Verify Installation**: Confirm configuration deployment

#### Demo Configuration Deployment
1. **Click "Deploy Demo"**
2. **Wait for Deployment**: Demo configuration is automatically deployed
3. **Enable Demo Mode**: Demo features are activated
4. **Verify Demo Setup**: Confirm demo scenarios and data

### Factory Reset

#### When to Reset
- System corruption or instability
- Configuration errors
- Data corruption issues
- Fresh installation requirements

#### Reset Process
1. **Click "Factory Reset"**
2. **Confirm Reset**: Acknowledge data loss warning
3. **Wait for Reset**: System clears all data and configuration
4. **Restart Setup**: Begin fresh installation process

---

## 🎯 Academic Demonstration

### Demo Mode Setup

#### Enabling Demo Mode
1. **Deploy Demo Configuration**: Use "Deploy Demo" button
2. **Verify Demo Data**: Check demo profiles and scenarios
3. **Test Demo Features**: Verify demo functionality
4. **Prepare Scenarios**: Set up demonstration scenarios

#### Demo Scenarios Available
- **Single Crash Scenario**: Basic crash detection and response
- **Multi-Crash Scenario**: Multiple incident coordination
- **Network Failure Scenario**: System resilience testing
- **ESP32 Disconnection**: Hardware failure handling
- **GPS Failure Scenario**: Location service fallback
- **Complete Emergency Response**: End-to-end demonstration

### Demonstration Execution

#### Pre-Demo Checklist
- [ ] Demo mode enabled
- [ ] Demo data deployed
- [ ] MQTT broker running
- [ ] Network connectivity verified
- [ ] ESP32 hardware connected (if applicable)
- [ ] Test devices prepared

#### Demo Flow
1. **Introduction**: Explain system purpose and capabilities
2. **Publisher Mode**: Demonstrate crash detection and alert broadcasting
3. **Subscriber Mode**: Show emergency response and incident management
4. **Integration**: Demonstrate end-to-end emergency workflow
5. **Q&A**: Address questions and demonstrate additional features

---

## 🛠️ Troubleshooting

### Common Issues

#### Build Issues
- **Keystore Errors**: Verify keystore configuration and passwords
- **Dependency Issues**: Check Gradle sync and dependency resolution
- **Compilation Errors**: Review code for syntax or import issues

#### Runtime Issues
- **App Crashes**: Check system logs and error reports
- **Performance Issues**: Monitor system resources and performance metrics
- **Network Issues**: Verify MQTT broker connectivity and network settings

#### Maintenance Issues
- **Backup Failures**: Check storage space and file permissions
- **Restore Failures**: Verify backup file integrity and format
- **Maintenance Errors**: Review maintenance logs and system status

### Getting Help

#### System Logs
- **Production Dashboard**: View real-time system status
- **Error Logs**: Check error history and details
- **Performance Reports**: Analyze system performance data

#### Support Resources
- **Documentation**: Refer to technical documentation
- **System Diagnostics**: Run comprehensive system diagnostics
- **Error Reports**: Export error logs for analysis

---

## 📚 Advanced Features

### Custom Configuration

#### Configuration Files
- **Settings**: App preferences and configuration
- **Profiles**: Medical profile templates
- **Scenarios**: Custom demonstration scenarios
- **Themes**: UI customization and theming

#### Configuration Deployment
1. **Create Config Package**: Organize configuration files
2. **Package as ZIP**: Compress configuration files
3. **Deploy Configuration**: Use installation manager
4. **Verify Deployment**: Confirm configuration application

### Performance Optimization

#### Monitoring Performance
- **Performance Dashboard**: Real-time performance metrics
- **Trend Analysis**: Historical performance data
- **Optimization Recommendations**: Automated optimization suggestions

#### System Tuning
- **Memory Optimization**: Adjust memory usage patterns
- **Database Optimization**: Optimize database queries and structure
- **Network Optimization**: Tune MQTT and network settings

---

## 🔒 Security Considerations

### Production Security

#### Keystore Management
- **Secure Storage**: Store keystore files securely
- **Password Management**: Use strong, unique passwords
- **Access Control**: Limit access to production signing keys

#### Network Security
- **MQTT Security**: Use secure MQTT connections (TLS/SSL)
- **Network Isolation**: Isolate MQTT broker on secure network
- **Access Control**: Implement proper authentication and authorization

### Data Protection

#### Sensitive Data
- **Medical Information**: Encrypt sensitive medical data
- **Location Data**: Protect user location privacy
- **Communication Security**: Secure MQTT message transmission

#### Backup Security
- **Encrypted Backups**: Use encryption for backup files
- **Secure Storage**: Store backups in secure locations
- **Access Control**: Limit backup access to authorized personnel

---

## 📋 Maintenance Schedule

### Recommended Maintenance

#### Daily
- **System Health Check**: Verify system status and performance
- **Error Log Review**: Check for new errors or issues
- **Performance Monitoring**: Monitor system performance metrics

#### Weekly
- **Scheduled Maintenance**: Run automated maintenance tasks
- **Backup Creation**: Create system backup
- **Performance Analysis**: Analyze performance trends

#### Monthly
- **System Diagnostics**: Run comprehensive system diagnostics
- **Configuration Review**: Review and update system configuration
- **Security Audit**: Check security settings and access controls

#### Quarterly
- **Major Updates**: Plan and execute major system updates
- **Performance Optimization**: Implement performance improvements
- **Documentation Update**: Update system documentation

---

## 🎉 Conclusion

The Car Crash Detection App provides a comprehensive, production-ready emergency response communication system suitable for academic demonstrations and real-world emergency scenarios. With proper deployment, monitoring, and maintenance, the system will provide reliable emergency communication capabilities while maintaining high performance and security standards.

For additional support or questions, refer to the technical documentation or contact the development team.

---

**Document Version**: 1.0  
**Last Updated**: December 2024  
**App Version**: 1.1.0  
**Compatibility**: Android 7.0+ (API 24+)
