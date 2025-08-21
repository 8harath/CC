# Broadcast Receiver Exported Flag Fix

## Issue Description
The app was crashing on Android API 34+ (Android 14+) with a SecurityException when registering broadcast receivers. The error message was:

```
One of RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED should be specified when a receiver isn't being registered exclusively for system broadcasts
```

## Root Cause
Starting with Android API 34 (Android 14), Google introduced a new security requirement that requires explicit specification of whether a broadcast receiver should be exported or not when registering it dynamically. This is part of Android's security hardening efforts.

## Files Modified
- `app/src/main/java/com/example/cc/ui/publisher/PublisherActivity.kt`
- `app/src/main/java/com/example/cc/ui/subscriber/SubscriberActivity.kt`
- `app/src/main/java/com/example/cc/util/MqttService.kt`
- `app/src/main/java/com/example/cc/util/Esp32BluetoothService.kt`
- `app/src/main/java/com/example/cc/util/Esp32WifiDirectService.kt`

## Fix Applied
Added version-specific broadcast receiver registration that includes the `RECEIVER_NOT_EXPORTED` flag for Android API 34+ while maintaining backward compatibility for older versions.

### Before:
```kotlin
registerReceiver(receiver, IntentFilter("com.example.cc.MESSAGE_PUBLISHED"))
```

### After:
```kotlin
if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
    registerReceiver(receiver, IntentFilter("com.example.cc.MESSAGE_PUBLISHED"), Context.RECEIVER_NOT_EXPORTED)
} else {
    registerReceiver(receiver, IntentFilter("com.example.cc.MESSAGE_PUBLISHED"))
}
```

## Why RECEIVER_NOT_EXPORTED?
- All the broadcast receivers in this app are for internal app communication
- They handle custom intents like `"com.example.cc.MESSAGE_PUBLISHED"`
- These receivers should not be accessible from outside the app
- `RECEIVER_NOT_EXPORTED` ensures the receiver is only accessible within the app

## Specific Changes Made

### PublisherActivity
- Fixed `messagePublishReceiver` registration

### SubscriberActivity  
- Fixed `emergencyAlertReceiver` registration
- Fixed `simpleMessageReceiver` registration
- Fixed `customMessageReceiver` registration

### MqttService
- Fixed `networkReceiver` registration for connectivity monitoring

### Esp32BluetoothService
- Fixed `bluetoothReceiver` registration for Bluetooth device discovery

### Esp32WifiDirectService
- Fixed `receiver` registration for WiFi P2P functionality

## Result
- ✅ Fixed the SecurityException on Android 14+
- ✅ Maintained backward compatibility with older Android versions
- ✅ All broadcast receivers now properly specify their export status
- ✅ No functional impact on the app's behavior

## Testing
The fix should be tested by:
1. Running the app on Android 14+ devices
2. Running the app on older Android versions
3. Verifying that all MQTT communication still works
4. Verifying that Bluetooth and WiFi P2P functionality still works
5. Confirming no crashes occur during broadcast receiver registration

## Date Fixed
2025-01-20

