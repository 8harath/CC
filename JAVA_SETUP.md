# Java Setup Guide for Android Development

## 🚨 Current Issue
The project requires Java Development Kit (JDK) 11 or higher to build. Currently, Java is not installed or not properly configured.

## 🔧 Quick Solutions

### Option 1: Install Java via Android Studio (Recommended)

1. **Open Android Studio**
2. **Go to**: File → Project Structure
3. **Select**: SDK Location (left sidebar)
4. **Under "JDK location"**: Click "Download JDK"
5. **Choose**: 
   - Version: 11 or higher
   - Vendor: Eclipse Temurin (recommended)
6. **Click**: Download
7. **Android Studio will automatically set JAVA_HOME**

### Option 2: Manual Java Installation

1. **Download OpenJDK 11**:
   - Go to: https://adoptium.net/
   - Download Eclipse Temurin JDK 11 for Windows
   - Run the installer

2. **Set JAVA_HOME Environment Variable**:
   - Open System Properties (Win + R, type `sysdm.cpl`)
   - Click "Environment Variables"
   - Under "System Variables", click "New"
   - Variable name: `JAVA_HOME`
   - Variable value: `C:\Program Files\Eclipse Adoptium\jdk-11.x.x.x-hotspot` (adjust path)
   - Add `%JAVA_HOME%\bin` to your PATH variable

3. **Verify Installation**:
   - Open new Command Prompt
   - Run: `java -version`
   - Should show Java version 11 or higher

### Option 3: Use Android Studio's Built-in JDK

If you have Android Studio installed, it usually comes with its own JDK:

1. **Find Android Studio's JDK**:
   - Usually located in: `C:\Program Files\Android\Android Studio\jbr`
   - Or: `C:\Users\[YourUsername]\AppData\Local\Android\Sdk\jbr`

2. **Set JAVA_HOME**:
   ```cmd
   set JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
   ```

## 🧪 Test the Setup

After installing Java, test with:

```cmd
java -version
javac -version
echo %JAVA_HOME%
```

## 🚀 Alternative: Use Android Studio Directly

If you prefer not to install Java separately:

1. **Open Android Studio**
2. **Open the project**: File → Open → Select your project folder
3. **Let Gradle sync complete**
4. **Connect your Android device** (enable USB Debugging)
5. **Click the green play button** ▶️

Android Studio will handle all the Java/JDK requirements automatically.

## 📱 Running the App

### Via Android Studio (Easiest)
1. Open project in Android Studio
2. Connect Android device (API 24+)
3. Click Run button
4. App will install and launch

### Via Command Line (After Java setup)
```cmd
# Clean and build
.\gradlew.bat clean
.\gradlew.bat assembleDebug

# Install on device
.\gradlew.bat installDebug
```

## 🔍 Troubleshooting

### "JAVA_HOME is not set"
- Follow the manual installation steps above
- Restart Command Prompt after setting environment variables

### "Gradle sync failed"
- In Android Studio: File → Invalidate Caches and Restart
- Check internet connection for dependency downloads

### "Build failed"
- Ensure JDK 11+ is installed
- Check Android Studio's SDK Manager for required SDK versions

## 📞 Need Help?

If you're still having issues:
1. Try Android Studio method first (easiest)
2. Check Android Studio's Event Log for specific errors
3. Ensure your Android device has Developer Options and USB Debugging enabled

---

**Recommended Approach**: Use Android Studio directly - it handles all Java/JDK requirements automatically! 