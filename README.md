# Car Crash Detection MQTT System - Phase 1

## Overview
This Android application serves as a dual-mode communication interface for a car crash detection system using MQTT protocol. The app operates in Publisher mode (crash victims) and Subscriber mode (emergency responders).

## Phase 1 Implementation Status ✅

### ✅ Completed Features
- **Project Structure**: MVVM architecture with Repository pattern
- **Base Activities/Fragments**: MainActivity, role selection, basic navigation
- **Data Layer**: Room database setup with User, MedicalProfile, and Incident entities
- **Dependency Injection**: Simple manual DI implementation
- **Basic UI Framework**: Material Design theming and base layouts
- **Role Selection System**: Complete UI for choosing between Publisher and Subscriber modes
- **Database Operations**: Full CRUD operations for all entities
- **Navigation**: Seamless flow between role selection and mode-specific activities

### 🏗️ Architecture Components
- **MVVM Pattern**: ViewModels with LiveData and StateFlow
- **Repository Pattern**: Clean separation between data sources and business logic
- **Room Database**: Local SQLite database with type converters
- **ViewBinding**: Type-safe view binding for all activities
- **Coroutines**: Asynchronous operations with proper error handling

### 📱 User Interface
- **Role Selection Screen**: Clean, modern interface for choosing user role
- **Publisher Mode**: Emergency alert interface with large SOS button
- **Subscriber Mode**: Status display for emergency responders
- **Material Design**: Consistent theming with custom color scheme
- **Responsive Layout**: Works across different screen sizes

### 🗄️ Data Models
- **User**: Stores user information and role (Publisher/Subscriber)
- **MedicalProfile**: Medical information for crash victims
- **Incident**: Crash incident data with status tracking

## Setup Instructions

### Prerequisites
1. **Android Studio**: Latest version (Arctic Fox or newer)
2. **Java Development Kit**: JDK 11 or higher
3. **Android Device/Emulator**: API level 24+ (Android 7.0+)
4. **Mosquitto MQTT Broker**: Already installed on your laptop

### Running the Application

1. **Open Project in Android Studio**
   ```bash
   # Open Android Studio and select "Open an existing project"
   # Navigate to the project directory and select it
   ```

2. **Sync Project**
   - Android Studio will automatically sync the project
   - Wait for Gradle sync to complete

3. **Set up Android Device/Emulator**
   - Connect an Android device via USB (enable Developer Options and USB Debugging)
   - OR create an Android Virtual Device (AVD) in Android Studio

4. **Run the Application**
   - Click the "Run" button (green play icon) in Android Studio
   - Select your device/emulator
   - The app will install and launch

### First Run Experience
1. **Role Selection**: Choose between "Crash Victim" or "Emergency Responder"
2. **Name Input**: Enter your name when prompted
3. **Mode-Specific Interface**: 
   - **Publisher Mode**: Large SOS button for emergency alerts
   - **Subscriber Mode**: Status display showing "MQTT not implemented yet"

## Project Structure

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
└── ui/
    ├── base/                        # Base classes
    ├── main/                        # Role selection
    ├── publisher/                   # Crash victim mode
    └── subscriber/                  # Emergency responder mode
```

## Key Features Implemented

### 🔐 Role-Based Access
- Users can select between Publisher (victim) and Subscriber (responder) modes
- Role selection persists across app sessions
- Different UI and functionality for each role

### 💾 Data Persistence
- Room database with automatic migrations
- User profiles stored locally
- Medical information management ready
- Incident tracking system in place

### 🎨 Modern UI/UX
- Material Design 3 components
- Custom color scheme for emergency context
- Large, accessible touch targets
- Clear visual hierarchy

### 🔧 Extensible Architecture
- Clean separation of concerns
- Easy to add new features
- Prepared for MQTT integration
- Ready for Bluetooth/WiFi integration

## Next Steps (Phase 2)

### MQTT Integration
- [ ] Eclipse Paho MQTT Android library integration
- [ ] Connection management with auto-reconnection
- [ ] Message publishing for emergency alerts
- [ ] Message subscription for responders
- [ ] Topic management system

### ESP32 Integration
- [ ] Bluetooth Classic and BLE communication
- [ ] WiFi direct fallback
- [ ] Sensor data parsing
- [ ] Device discovery and pairing

### Enhanced Features
- [ ] GPS location services
- [ ] Medical profile management UI
- [ ] Emergency contact system
- [ ] Real-time incident monitoring

## Troubleshooting

### Common Issues

1. **Gradle Sync Fails**
   - Check internet connection
   - Invalidate caches and restart (File → Invalidate Caches)
   - Update Android Studio to latest version

2. **Build Errors**
   - Ensure JDK 11+ is installed and JAVA_HOME is set
   - Clean and rebuild project (Build → Clean Project)

3. **App Crashes on Launch**
   - Check logcat for specific error messages
   - Ensure all permissions are granted
   - Verify device API level compatibility

### Development Tips

1. **Database Inspection**
   - Use Android Studio's Database Inspector to view Room database
   - Located in View → Tool Windows → App Inspection

2. **Logging**
   - All ViewModels include error handling and logging
   - Check logcat for detailed error messages

3. **Testing**
   - Test on both physical device and emulator
   - Verify different screen sizes and orientations

## Configuration

### MQTT Broker Settings (Future)
The app is prepared for MQTT integration with these default settings:
- **Broker**: localhost (your laptop)
- **Port**: 1883 (default Mosquitto port)
- **Topics**: 
  - `emergency/alerts` (for emergency broadcasts)
  - `emergency/status` (for status updates)
  - `emergency/response` (for responder acknowledgments)

### Permissions
The app requests these permissions:
- `INTERNET`: For MQTT communication
- `ACCESS_NETWORK_STATE`: For network monitoring
- `WAKE_LOCK`: For background MQTT operations

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review Android Studio logcat for error details
3. Ensure all prerequisites are properly installed

---

**Phase 1 Status**: ✅ Complete and Ready for Testing
**Next Phase**: MQTT Communication Core (Phase 2) 