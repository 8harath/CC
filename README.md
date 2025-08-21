# Car Crash Detection MQTT System

## Overview
This Android application serves as a dual-mode communication interface for a car crash detection system using MQTT protocol. The app operates in Publisher mode (crash victims) and Subscriber mode (emergency responders), integrating with ESP32 hardware and a local MQTT broker for academic demonstration purposes.

## 🚀 Quick Start

### Prerequisites
- Android Studio (latest version)
- JDK 11 or higher
- Android device/emulator (API level 24+)
- Local MQTT broker (Mosquitto recommended)

### Installation & Setup

1. **Clone and Open Project**
   ```bash
   git clone <repository-url>
   cd CC
   # Open in Android Studio
   ```

2. **Configure MQTT Broker**
   - Install Mosquitto MQTT broker
   - Update broker URL in `app/src/main/java/com/example/cc/util/MqttConfig.kt`
   - Run `scripts/setup_local_mqtt.bat` (Windows) or `scripts/setup_local_mqtt.sh` (Linux/Mac)

3. **Build and Run**
   ```bash
   # Using Android Studio
   # Click "Run" button or use:
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

4. **First Launch**
   - Select role: "Crash Victim" (Publisher) or "Emergency Responder" (Subscriber)
   - Enter your name
   - Configure MQTT settings if needed

## 📱 Features

### Publisher Mode (Crash Victims)
- Emergency alert system with SOS button
- Medical profile management
- GPS location tracking
- ESP32 sensor integration
- Real-time MQTT communication

### Subscriber Mode (Emergency Responders)
- Live incident monitoring
- Alert history and details
- Response acknowledgment system
- Emergency contact management
- Real-time status updates

### Core Features
- **MVVM Architecture**: Clean separation of concerns
- **Room Database**: Local data persistence
- **MQTT Communication**: Real-time messaging
- **Bluetooth Integration**: ESP32 hardware communication
- **GPS Services**: Location tracking
- **Material Design**: Modern, accessible UI

## 🏗️ Architecture

```
app/src/main/java/com/example/cc/
├── CarCrashDetectionApp.kt          # Application class
├── data/
│   ├── dao/                         # Room DAOs
│   ├── database/                    # Room database setup
│   ├── model/                       # Data entities
│   ├── repository/                  # Repository layer
│   └── util/                        # Type converters
├── di/
│   └── AppModule.kt                 # Dependency injection
├── ui/
│   ├── base/                        # Base classes
│   ├── main/                        # Role selection
│   ├── publisher/                   # Crash victim mode
│   ├── subscriber/                  # Emergency responder mode
│   ├── settings/                    # MQTT configuration
│   └── testing/                     # MQTT testing interface
└── util/
    ├── MqttService.kt               # MQTT communication
    ├── Esp32Manager.kt              # ESP32 integration
    ├── GpsService.kt                # Location services
    └── SystemHealthMonitor.kt       # System monitoring
```

## 🔧 Configuration

### MQTT Settings
Default configuration in `MqttConfig.kt`:
```kotlin
const val BROKER_URL = "tcp://test.mosquitto.org:1883"
const val CLIENT_ID = "android_client_${System.currentTimeMillis()}"
```

### ESP32 Integration
- Bluetooth Classic and BLE support
- WiFi Direct fallback
- MPU6050 sensor integration
- Automatic device discovery

### Permissions
- `INTERNET`: MQTT communication
- `ACCESS_NETWORK_STATE`: Network monitoring
- `ACCESS_FINE_LOCATION`: GPS services
- `BLUETOOTH`: ESP32 communication
- `WAKE_LOCK`: Background operations

## 📚 Documentation

- **[Quick Start Guide](docs/QUICK_START.md)**: Get up and running quickly
- **[Setup Guide](docs/SETUP_GUIDE.md)**: Complete ESP32 and MQTT setup
- **[Production Guide](docs/PRODUCTION_GUIDE.md)**: Production deployment guide
- **[Development Plan](docs/DEVELOPMENT_PLAN.md)**: Project roadmap and phases

## 🧪 Testing

### MQTT Testing
```bash
# Test local broker
scripts/test_mqtt_local_broker.bat

# Test connection
scripts/test_mqtt_connection.bat

# Test communication
scripts/diagnose_mqtt_communication.bat
```

### Bluetooth Testing
```bash
# Test ESP32 setup
scripts/test_bluetooth_setup.bat
```

### Python Test Scripts
- `scripts/test_mqtt_broker.py`: MQTT broker connectivity
- `scripts/test_local_broker.py`: Local broker testing
- `scripts/test_ip_validation_and_messaging.py`: Network validation

## 🚀 Production Deployment

### Building Production APK
```bash
# Windows
scripts/build_production.bat

# Linux/Mac
./scripts/build_production.sh

# Manual
./gradlew assembleRelease
```

### Production Features
- Optimized performance
- ProGuard code obfuscation
- Release signing
- Production monitoring dashboard
- System health monitoring

## 🐛 Troubleshooting

### Common Issues

1. **MQTT Connection Fails**
   - Verify broker is running: `scripts/check_mosquitto.bat`
   - Check network connectivity
   - Verify broker URL in MqttConfig.kt

2. **ESP32 Not Connecting**
   - Ensure ESP32 is powered and in range
   - Check Bluetooth permissions
   - Verify ESP32 firmware is uploaded

3. **App Crashes**
   - Check logcat for error details
   - Verify device API level compatibility
   - Clear app data if needed

### Debug Tools
- MQTT Test Activity: Built-in MQTT testing interface
- System Health Monitor: Real-time system status
- Database Inspector: View Room database contents
- Logcat: Detailed error logging

## 📄 License

This project is developed for academic demonstration purposes.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📞 Support

For issues or questions:
1. Check the troubleshooting section
2. Review Android Studio logcat
3. Ensure all prerequisites are installed
4. Test with provided test scripts

---

**Status**: ✅ Production Ready  
**Version**: 1.0.0  
**Last Updated**: December 2024