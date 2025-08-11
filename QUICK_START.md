# Quick Start Guide - Phase 1

## 🚀 Running the Car Crash Detection App

### Option 1: Android Studio (Recommended)

1. **Open Android Studio**
2. **Open Project**: File → Open → Select the project folder
3. **Wait for Sync**: Let Gradle sync complete
4. **Connect Device**: 
   - Enable Developer Options on your Android phone
   - Enable USB Debugging
   - Connect via USB cable
5. **Run App**: Click the green play button ▶️

### Option 2: Direct APK Build

1. **Set up Java**: Install JDK 11+ and set JAVA_HOME
2. **Run Build Script**: Double-click `build_apk.bat`
3. **Install APK**: Transfer `app-debug.apk` to your device and install

### Option 3: Command Line

```bash
# Clean and build
.\gradlew.bat clean
.\gradlew.bat assembleDebug

# Install on connected device
.\gradlew.bat installDebug
```

## 📱 App Features to Test

### 1. Role Selection Screen
- ✅ Choose between "Crash Victim" and "Emergency Responder"
- ✅ Visual feedback when selecting roles
- ✅ Continue button enables only after selection

### 2. Name Input Dialog
- ✅ Enter your name when prompted
- ✅ Validation prevents empty names
- ✅ User data saved to local database

### 3. Publisher Mode (Crash Victim)
- ✅ Large SOS emergency button
- ✅ Red color scheme for emergency context
- ✅ Button shows "Emergency alert sent!" message
- ✅ Back navigation to role selection

### 4. Subscriber Mode (Emergency Responder)
- ✅ Blue color scheme for responder context
- ✅ Status display showing "MQTT not implemented yet"
- ✅ Back navigation to role selection

### 5. Database Operations
- ✅ User creation and storage
- ✅ Role persistence across app sessions
- ✅ Database test logs in Android Studio logcat

## 🔍 Testing Checklist

- [ ] App launches without crashes
- [ ] Role selection works correctly
- [ ] Name input dialog appears and validates
- [ ] Navigation between screens works
- [ ] Database operations complete successfully
- [ ] UI looks good on different screen sizes
- [ ] Back navigation works from both modes

## 🐛 Troubleshooting

### App Won't Launch
- Check logcat for error messages
- Ensure device API level is 24+ (Android 7.0+)
- Verify all permissions are granted

### Build Errors
- Install JDK 11+ and set JAVA_HOME
- Update Android Studio to latest version
- Invalidate caches: File → Invalidate Caches

### Database Issues
- Check logcat for "DatabaseTest" messages
- Verify Room database initialization
- Clear app data if needed

## 📊 What's Working

✅ **Complete Phase 1 Implementation**
- MVVM architecture with Repository pattern
- Room database with full CRUD operations
- Role-based navigation system
- Material Design UI components
- Error handling and loading states
- Database testing and validation

## 🎯 Next Steps

The app is ready for **Phase 2: MQTT Communication Core**
- MQTT client integration
- Connection management
- Message publishing/subscription
- Real-time communication with your Mosquitto broker

---

**Status**: ✅ Phase 1 Complete - Ready for Testing! 