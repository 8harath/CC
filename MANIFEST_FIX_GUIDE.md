# Android Manifest Fix Guide

## 🚨 Issue: "Unable to find explicit activity class"

**Error Message**: `Error opening MQTT settings. unable to find explicit activityclass (com.example..`

## 🔍 Root Cause

The `MqttSettingsActivity` and other activities were missing from the `AndroidManifest.xml` file. Android requires all activities to be explicitly declared in the manifest.

## ✅ Fix Applied

### Added Missing Activities to AndroidManifest.xml:

```xml
<!-- MQTT Settings Activity -->
<activity
    android:name=".ui.settings.MqttSettingsActivity"
    android:exported="false"
    android:theme="@style/Theme.CC" />

<!-- MQTT Test Activity -->
<activity
    android:name=".ui.testing.MqttTestActivity"
    android:exported="false"
    android:theme="@style/Theme.CC" />

<!-- Medical Profile Editor Activity -->
<activity
    android:name=".ui.publisher.MedicalProfileEditorActivity"
    android:exported="false"
    android:theme="@style/Theme.CC" />
```

## 📱 Activities Now Available

After the fix, these activities are properly declared:

1. **MainActivity** - App launcher
2. **PublisherActivity** - Crash victim mode
3. **SubscriberActivity** - Emergency responder mode
4. **IncidentDetailActivity** - View incident details
5. **MqttSettingsActivity** - Configure MQTT settings ⭐ **FIXED**
6. **MqttTestActivity** - Test MQTT communication ⭐ **FIXED**
7. **MedicalProfileEditorActivity** - Edit medical profiles ⭐ **FIXED**

## 🔧 How to Apply the Fix

### Option 1: Use the Updated Manifest
The `AndroidManifest.xml` file has been updated with all missing activities.

### Option 2: Manual Fix
If you need to add activities manually:

1. Open `app/src/main/AndroidManifest.xml`
2. Add the missing activity declarations inside the `<application>` tag
3. Rebuild the project

## 🧪 Testing the Fix

### Test MQTT Settings:
1. Launch the app
2. Go to Settings → MQTT Settings
3. Should open without errors
4. Configure broker IP and port
5. Test connection

### Test Other Activities:
1. **Medical Profile Editor**: Should open from Publisher mode
2. **MQTT Test**: Should be accessible for testing
3. **All navigation**: Should work without crashes

## 🎯 Expected Results

### Before Fix:
- ❌ "Unable to find explicit activity class" error
- ❌ MQTT Settings won't open
- ❌ App crashes when trying to access settings

### After Fix:
- ✅ MQTT Settings opens normally
- ✅ All activities accessible
- ✅ No more activity-related crashes
- ✅ Full app functionality available

## 📋 Verification Checklist

- [ ] App launches without errors
- [ ] MQTT Settings opens successfully
- [ ] Medical Profile Editor opens from Publisher mode
- [ ] MQTT Test activity accessible
- [ ] All navigation between activities works
- [ ] No "explicit activity class" errors

## 🔍 If Issues Persist

### Check Build:
1. Clean and rebuild project
2. Invalidate caches and restart Android Studio
3. Check for any remaining compilation errors

### Verify Manifest:
1. Ensure all activities are properly declared
2. Check for typos in activity names
3. Verify package names are correct

### Check Logs:
```bash
adb logcat | grep "ActivityManager"
```

## 📞 Support

If you still encounter issues:
1. Check that the manifest file was properly updated
2. Verify all activity names match the actual class names
3. Clean and rebuild the project
4. Check Android Studio logs for additional errors

---

**🎯 Status**: ✅ **MANIFEST ISSUE RESOLVED**

The MQTT Settings and other activities should now open without the "explicit activity class" error.
