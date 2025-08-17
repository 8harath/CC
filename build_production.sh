#!/bin/bash

echo "========================================"
echo "Car Crash Detection App - Production Build"
echo "========================================"
echo

# Check Java installation
echo "Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH"
    echo "Please install Java 11 or later and add it to PATH"
    exit 1
fi

# Check Android SDK
echo "Checking Android SDK..."
if [ -z "$ANDROID_HOME" ]; then
    echo "ERROR: ANDROID_HOME environment variable is not set"
    echo "Please set ANDROID_HOME to your Android SDK path"
    exit 1
fi

# Check keystore configuration
echo "Checking keystore configuration..."
if [ ! -f "keystore.properties" ]; then
    echo "ERROR: keystore.properties file not found"
    echo "Please create keystore.properties with your signing configuration"
    exit 1
fi

echo
echo "Starting production build..."
echo

# Clean previous builds
echo "Cleaning previous builds..."
./gradlew clean
if [ $? -ne 0 ]; then
    echo "ERROR: Clean failed"
    exit 1
fi

# Build release APK
echo "Building release APK..."
./gradlew assembleRelease
if [ $? -ne 0 ]; then
    echo "ERROR: Build failed"
    exit 1
fi

echo
echo "Checking for generated APK..."
if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    echo
    echo "========================================"
    echo "BUILD SUCCESSFUL!"
    echo "========================================"
    echo
    echo "Production APK generated at:"
    echo "app/build/outputs/apk/release/app-release.apk"
    echo
    echo "APK Details:"
    ls -la "app/build/outputs/apk/release/app-release.apk"
    echo
    echo "Ready for distribution!"
    echo
else
    echo "ERROR: APK not found in expected location"
    echo "Check build output for errors"
    exit 1
fi

echo
echo "Build completed successfully!"
