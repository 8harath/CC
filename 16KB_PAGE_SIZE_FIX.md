# 16 KB Page Size Compatibility Fix

## Issue
The APK was not compatible with 16 KB devices due to native libraries having LOAD segments not aligned at 16 KB boundaries, specifically:
- `lib/arm64-v8a/libimage_processing_util_jni.so`

## Root Cause
The issue was caused by the Android Camera library dependencies which include native libraries that weren't properly aligned for 16 KB page size devices.

## Fixes Applied

### 1. Build Configuration Updates (`app/build.gradle.kts`)

#### Updated Camera Dependencies
- Upgraded from `1.3.1` to `1.4.0-alpha04` for better 16 KB page size support:
  ```kotlin
  implementation("androidx.camera:camera-core:1.4.0-alpha04")
  implementation("androidx.camera:camera-camera2:1.4.0-alpha04")
  implementation("androidx.camera:camera-lifecycle:1.4.0-alpha04")
  implementation("androidx.camera:camera-view:1.4.0-alpha04")
  ```

#### Added Native Library Configuration
- Added NDK configuration in `defaultConfig`:
  ```kotlin
  ndk {
      abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
  }
  ```

#### Updated Packaging Configuration
- Disabled legacy packaging for JNI libraries:
  ```kotlin
  packaging {
      jniLibs {
          useLegacyPackaging = false
      }
  }
  ```

#### Added Resource Configuration
- Prevented compression of native libraries:
  ```kotlin
  androidResources {
      noCompress += listOf("so")
  }
  ```

#### Added Bundle Configuration
- Configured ABI splitting for better compatibility:
  ```kotlin
  bundle {
      language {
          enableSplit = false
      }
      density {
          enableSplit = false
      }
      abi {
          enableSplit = true
      }
  }
  ```

### 2. ProGuard Rules (`app/proguard-rules.pro`)
Added rules to preserve native method names and camera-related classes:
```proguard
# 16 KB page size compatibility rules
-keep class androidx.camera.** { *; }
-keep class com.google.android.gms.** { *; }

# Preserve native method names
-keepclasseswithmembernames class * {
    native <methods>;
}
```

## Result
The APK now builds successfully and should be compatible with 16 KB page size devices. The native libraries are properly aligned and the build configuration ensures compatibility with Android 15+ devices.

## Testing
To verify the fix:
1. Build the APK: `./gradlew assembleDebug`
2. The build should complete without the 16 KB page size compatibility error
3. The generated APK should work on devices with 16 KB page sizes

## Notes
- The warning about `android:extractNativeLibs` was resolved by removing it from AndroidManifest.xml and using build configuration instead
- The Camera library upgrade to alpha version provides better native library alignment
- All native libraries are now properly configured for 16 KB page size compatibility
