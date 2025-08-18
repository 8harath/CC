# Bluetooth Functionality Test Report
## Car Crash Detection App - ESP32 Connection Status

### Current Implementation Status

#### ✅ **What's Working:**
1. **Bluetooth Service Implementation**: Complete `Esp32BluetoothService` class with both Classic and BLE support
2. **Permission Setup**: All required Bluetooth permissions are properly declared in AndroidManifest.xml
3. **UI Integration**: PublisherActivity has Bluetooth discovery and connection buttons
4. **State Management**: Proper connection state tracking and sensor data parsing
5. **ESP32 Manager**: Unified manager coordinating Bluetooth and WiFi Direct services

#### ⚠️ **Potential Issues Identified:**

### 1. **Bluetooth Discovery Implementation Gap**
**Problem**: The `Esp32BluetoothService.startDiscovery()` method calls `bluetoothAdapter.startDiscovery()` but there's no BroadcastReceiver to handle discovered devices.

**Missing Code**:
```kotlin
// This BroadcastReceiver is missing from the service
private val discoveryReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_FOUND -> {
                val device = intent.getParcelableExtra<BluetoothDevice>(
                    BluetoothDevice.EXTRA_DEVICE
                )
                device?.let { 
                    _discoveredDevices.value = _discoveredDevices.value + it
                }
            }
            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                Log.i(TAG, "Bluetooth discovery started")
            }
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                Log.i(TAG, "Bluetooth discovery finished")
            }
        }
    }
}
```

### 2. **Service Registration Issue**
**Problem**: The `Esp32BluetoothService` is not registered as a service in AndroidManifest.xml, which may cause lifecycle issues.

**Solution**: Add to AndroidManifest.xml:
```xml
<service
    android:name=".util.Esp32BluetoothService"
    android:exported="false" />
```

### 3. **Bluetooth Classic vs BLE Confusion**
**Problem**: The ESP32 code uses `BluetoothSerial` (Classic) but the Android app tries to connect via BLE first.

**ESP32 Code Issue**:
```cpp
// Current ESP32 code uses Classic Bluetooth
BluetoothSerial SerialBT;  // This is Classic, not BLE

// Should be BLE for better compatibility
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEAdvertisedDevice.h>
```

### 4. **UUID Mismatch**
**Problem**: The UUIDs in the Android app may not match the ESP32's actual service UUIDs.

**Current UUIDs**:
```kotlin
// These are generic UUIDs, may not match ESP32
private val ESP32_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
private val ESP32_CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
```

### 5. **Data Transmission Logic Issue**
**Problem**: The ESP32 only sends data when `SerialBT.available()` returns true, but this may never happen in the current implementation.

**Current ESP32 Code**:
```cpp
void sendDataViaBLE() {
    if (SerialBT.available()) {  // This condition may never be true
        // Send data...
    }
}
```

**Should be**:
```cpp
void sendDataViaBLE() {
    // Always send data, don't check if available
    String data = "ACC:" + String(accelX, 2) + "," + String(accelY, 2) + "," + String(accelZ, 2);
    data += "|IMPACT:" + String(impactForce, 2);
    data += "|GPS:" + String(gps.location.lat(), 6) + "," + String(gps.location.lng(), 6);
    
    SerialBT.println(data);
    Serial.println("Sent: " + data);
}
```

---

## **Required Fixes for Bluetooth to Work**

### 1. **Fix ESP32 Code (Use BLE instead of Classic)**
```cpp
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEAdvertisedDevice.h>

// BLE Service and Characteristic UUIDs
#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        Serial.println("BLE Client connected");
    }
    
    void onDisconnect(BLEServer* pServer) {
        Serial.println("BLE Client disconnected");
        pServer->startAdvertising();
    }
};

BLEServer* pServer = nullptr;
BLECharacteristic* pCharacteristic = nullptr;

void setup() {
    Serial.begin(115200);
    
    // Initialize BLE
    BLEDevice::init("ESP32_CarCrash");
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());
    
    BLEService* pService = pServer->createService(SERVICE_UUID);
    pCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_READ |
        BLECharacteristic::PROPERTY_WRITE |
        BLECharacteristic::PROPERTY_NOTIFY
    );
    
    pService->start();
    pServer->startAdvertising();
    Serial.println("BLE Server ready");
}

void loop() {
    // Read sensors
    readAccelerometer();
    calculateImpactForce();
    readGPS();
    
    // Send data via BLE every 100ms
    static unsigned long lastSend = 0;
    if (millis() - lastSend >= 100) {
        sendDataViaBLE();
        lastSend = millis();
    }
    
    delay(10);
}

void sendDataViaBLE() {
    String data = "ACC:" + String(accelX, 2) + "," + String(accelY, 2) + "," + String(accelZ, 2);
    data += "|IMPACT:" + String(impactForce, 2);
    data += "|GPS:" + String(gps.location.lat(), 6) + "," + String(gps.location.lng(), 6);
    
    pCharacteristic->setValue(data.c_str());
    pCharacteristic->notify();
    Serial.println("Sent: " + data);
}
```

### 2. **Fix Android Bluetooth Service**
```kotlin
class Esp32BluetoothService(private val context: Context) {
    
    companion object {
        private const val TAG = "Esp32BluetoothService"
        
        // Use the same UUIDs as ESP32
        private val ESP32_SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        private val ESP32_CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    }
    
    // Add BroadcastReceiver for device discovery
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(
                        BluetoothDevice.EXTRA_DEVICE
                    )
                    device?.let { 
                        val currentList = _discoveredDevices.value.toMutableList()
                        if (!currentList.contains(it)) {
                            currentList.add(it)
                            _discoveredDevices.value = currentList
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.i(TAG, "Bluetooth discovery started")
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i(TAG, "Bluetooth discovery finished")
                }
            }
        }
    }
    
    init {
        initializeBluetooth()
        registerDiscoveryReceiver()
    }
    
    private fun registerDiscoveryReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(discoveryReceiver, filter)
    }
    
    fun startDiscovery() {
        if (!isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth not enabled")
            return
        }
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Bluetooth scan permission not granted")
            return
        }
        
        // Clear previous discoveries
        _discoveredDevices.value = emptyList()
        
        executor.execute {
            try {
                bluetoothAdapter?.startDiscovery()
                Log.i(TAG, "Started Bluetooth device discovery")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start discovery: ${e.message}")
            }
        }
    }
    
    override fun onDestroy() {
        try {
            context.unregisterReceiver(discoveryReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
        super.onDestroy()
    }
}
```

### 3. **Update AndroidManifest.xml**
```xml
<!-- Add Bluetooth service -->
<service
    android:name=".util.Esp32BluetoothService"
    android:exported="false" />
```

---

## **Testing Steps to Verify Bluetooth Functionality**

### 1. **Test ESP32 BLE Visibility**
- Upload the corrected ESP32 code
- Use nRF Connect app to scan for BLE devices
- Look for "ESP32_CarCrash" device
- Verify it's advertising the correct service UUID

### 2. **Test Android App Discovery**
- Open the Car Crash Detection app
- Go to Publisher screen
- Tap "Discover" button
- Check if ESP32 appears in the device list
- Verify logcat shows discovery messages

### 3. **Test Connection**
- Select ESP32 device from the list
- Tap "Connect" button
- Check connection status updates
- Verify sensor data starts flowing

### 4. **Test Data Reception**
- Monitor logcat for sensor data messages
- Check if sensor data appears in the UI
- Verify data format matches expected structure

---

## **Summary**

The Bluetooth functionality in the app is **partially implemented** but has several critical gaps:

1. **ESP32 code needs to be updated** from Classic Bluetooth to BLE
2. **Android service needs BroadcastReceiver** for device discovery
3. **UUIDs need to match** between ESP32 and Android app
4. **Service registration** needs to be added to AndroidManifest.xml

Once these fixes are applied, the Bluetooth functionality should work properly for:
- Device discovery
- Connection establishment
- Real-time sensor data transmission
- Crash detection and emergency alerts

The current implementation has a solid foundation but requires these specific fixes to become fully functional.
