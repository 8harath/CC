# ESP32 BLE + MQTT Connection Guide
## Car Crash Detection App - Two Phone Communication Setup

### Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [ESP32 Setup](#esp32-setup)
4. [Android App Setup](#android-app-setup)
5. [BLE Connection Process](#ble-connection-process)
6. [MQTT Communication Setup](#mqtt-communication-setup)
7. [Two Phone Communication](#two-phone-communication)
8. [Testing and Troubleshooting](#testing-and-troubleshooting)
9. [Security Considerations](#security-considerations)
10. [Advanced Configuration](#advanced-configuration)

---

## Overview

This guide explains how to connect an ESP32 device to the Car Crash Detection Android app via Bluetooth Low Energy (BLE) and establish MQTT communication between two phones for emergency alert broadcasting and response coordination.

**System Architecture:**
- **ESP32 Device**: Collects sensor data (accelerometer, GPS) and sends via BLE
- **Phone 1 (Publisher)**: Receives ESP32 data, detects crashes, publishes emergency alerts
- **Phone 2 (Subscriber)**: Receives emergency alerts, displays incident details, coordinates response
- **MQTT Broker**: Facilitates real-time communication between phones

---

## Prerequisites

### Hardware Requirements
- ESP32 development board (ESP32-WROOM-32 or similar)
- Accelerometer sensor (MPU6050 or built-in)
- GPS module (NEO-6M or similar)
- USB cable for programming
- Two Android phones (Android 6.0+)

### Software Requirements
- Arduino IDE with ESP32 board support
- Mosquitto MQTT broker (local or cloud)
- Android Studio
- Car Crash Detection app (built APK)

### Network Requirements
- Both phones must be on the same WiFi network
- MQTT broker accessible from both phones
- Port 1883 open for MQTT communication

---

## ESP32 Setup

### 1. Install ESP32 Board Support
```
Arduino IDE → Tools → Board → Boards Manager
Search: "esp32" → Install "ESP32 by Espressif Systems"
```

### 2. ESP32 Code Implementation

```cpp
#include <BluetoothSerial.h>
#include <Wire.h>
#include <MPU6050.h>
#include <TinyGPS++.h>
#include <HardwareSerial.h>

// Pin definitions
#define MPU6050_SDA 21
#define MPU6050_SCL 22
#define GPS_RX 16
#define GPS_TX 17

// Objects
BluetoothSerial SerialBT;
MPU6050 mpu;
TinyGPSPlus gps;
HardwareSerial GPSSerial(1);

// Variables
float accelX, accelY, accelZ;
float impactForce = 0.0f;
float lastImpactForce = 0.0f;
unsigned long lastSendTime = 0;
const unsigned long SEND_INTERVAL = 100; // 100ms

void setup() {
  Serial.begin(115200);
  
  // Initialize I2C for MPU6050
  Wire.begin(MPU6050_SDA, MPU6050_SCL);
  
  // Initialize MPU6050
  mpu.initialize();
  if (!mpu.testConnection()) {
    Serial.println("MPU6050 connection failed");
  }
  
  // Initialize GPS
  GPSSerial.begin(9600, SERIAL_8N1, GPS_RX, GPS_TX);
  
  // Initialize Bluetooth
  SerialBT.begin("ESP32_CarCrash");
  Serial.println("Bluetooth device ready");
}

void loop() {
  // Read accelerometer data
  readAccelerometer();
  
  // Calculate impact force
  calculateImpactForce();
  
  // Read GPS data
  readGPS();
  
  // Send data via BLE every 100ms
  if (millis() - lastSendTime >= SEND_INTERVAL) {
    sendDataViaBLE();
    lastSendTime = millis();
  }
  
  delay(10);
}

void readAccelerometer() {
  int16_t ax, ay, az;
  mpu.getAcceleration(&ax, &ay, &az);
  
  // Convert to g-force
  accelX = ax / 16384.0f;
  accelY = ay / 16384.0f;
  accelZ = az / 16384.0f;
}

void calculateImpactForce() {
  // Calculate magnitude of acceleration
  float magnitude = sqrt(accelX*accelX + accelY*accelY + accelZ*accelZ);
  
  // Apply low-pass filter
  impactForce = 0.9f * lastImpactForce + 0.1f * magnitude;
  lastImpactForce = impactForce;
}

void readGPS() {
  while (GPSSerial.available()) {
    gps.encode(GPSSerial.read());
  }
}

void sendDataViaBLE() {
  if (SerialBT.available()) {
    // Format: "ACC:x,y,z|IMPACT:force|GPS:lat,lon"
    String data = "ACC:";
    data += String(accelX, 2) + ",";
    data += String(accelY, 2) + ",";
    data += String(accelZ, 2);
    data += "|IMPACT:";
    data += String(impactForce, 2);
    data += "|GPS:";
    
    if (gps.location.isValid()) {
      data += String(gps.location.lat(), 6) + ",";
      data += String(gps.location.lng(), 6);
    } else {
      data += "0,0";
    }
    
    SerialBT.println(data);
    Serial.println("Sent: " + data);
  }
}
```

### 3. ESP32 Configuration
- **Bluetooth Name**: "ESP32_CarCrash"
- **Service UUID**: `0000ffe0-0000-1000-8000-00805f9b34fb`
- **Characteristic UUID**: `0000ffe1-0000-1000-8000-00805f9b34fb`
- **Data Format**: `ACC:x,y,z|IMPACT:force|GPS:lat,lon`

---

## Android App Setup

### 1. Install the App
- Transfer the built APK to both phones
- Install the app on both devices
- Grant necessary permissions (Bluetooth, Location, Internet)

### 2. Required Permissions
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 3. App Configuration
- **Phone 1**: Set as "Publisher" role
- **Phone 2**: Set as "Subscriber" role
- Both phones must connect to the same MQTT broker

---

## BLE Connection Process

### 1. Enable Bluetooth
```kotlin
// Check if Bluetooth is enabled
if (!bluetoothAdapter.isEnabled) {
    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
}
```

### 2. Discover ESP32 Device
```kotlin
// Start device discovery
bluetoothAdapter.startDiscovery()

// Listen for discovered devices
val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            BluetoothDevice.ACTION_FOUND -> {
                val device = intent.getParcelableExtra<BluetoothDevice>(
                    BluetoothDevice.EXTRA_DEVICE
                )
                if (device?.name == "ESP32_CarCrash") {
                    // ESP32 found, connect to it
                    connectToESP32(device)
                }
            }
        }
    }
}
```

### 3. Establish BLE Connection
```kotlin
fun connectToESP32(device: BluetoothDevice) {
    val gatt = device.connectGatt(context, false, gattCallback)
}

private val gattCallback = object : BluetoothGattCallback() {
    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        when (newState) {
            BluetoothGatt.STATE_CONNECTED -> {
                Log.i("BLE", "Connected to ESP32")
                gatt?.discoverServices()
            }
            BluetoothGatt.STATE_DISCONNECTED -> {
                Log.i("BLE", "Disconnected from ESP32")
            }
        }
    }
    
    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            val service = gatt?.getService(ESP32_SERVICE_UUID)
            val characteristic = service?.getCharacteristic(ESP32_CHARACTERISTIC_UUID)
            
            if (characteristic != null) {
                gatt.setCharacteristicNotification(characteristic, true)
                Log.i("BLE", "BLE services discovered and notifications enabled")
            }
        }
    }
    
    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
        characteristic?.let { char ->
            if (char.uuid == ESP32_CHARACTERISTIC_UUID) {
                val data = char.value
                parseSensorData(data)
            }
        }
    }
}
```

### 4. Parse Sensor Data
```kotlin
private fun parseSensorData(data: ByteArray) {
    val message = String(data)
    Log.d("BLE", "Received: $message")
    
    // Parse format: "ACC:x,y,z|IMPACT:force|GPS:lat,lon"
    val parts = message.split("|")
    for (part in parts) {
        when {
            part.startsWith("ACC:") -> {
                val accValues = part.substring(4).split(",")
                if (accValues.size >= 3) {
                    val accX = accValues[0].toFloatOrNull() ?: 0f
                    val accY = accValues[1].toFloatOrNull() ?: 0f
                    val accZ = accValues[2].toFloatOrNull() ?: 0f
                    processAccelerometerData(accX, accY, accZ)
                }
            }
            part.startsWith("IMPACT:") -> {
                val impactForce = part.substring(7).toFloatOrNull() ?: 0f
                processImpactData(impactForce)
            }
            part.startsWith("GPS:") -> {
                val gpsValues = part.substring(4).split(",")
                if (gpsValues.size >= 2) {
                    val lat = gpsValues[0].toDoubleOrNull()
                    val lon = gpsValues[1].toDoubleOrNull()
                    processGPSData(lat, lon)
                }
            }
        }
    }
}
```

---

## MQTT Communication Setup

### 1. MQTT Broker Configuration
```bash
# Install Mosquitto on your local machine or use cloud service
# Local installation (Ubuntu/Debian):
sudo apt-get install mosquitto mosquitto-clients

# Start Mosquitto service
sudo systemctl start mosquitto
sudo systemctl enable mosquitto

# Configure Mosquitto (optional)
sudo nano /etc/mosquitto/mosquitto.conf
```

**Basic Mosquitto Configuration:**
```conf
# mosquitto.conf
listener 1883
allow_anonymous true
persistence false
log_type none
```

### 2. MQTT Topics Structure
```kotlin
object MqttTopics {
    const val EMERGENCY_ALERTS = "emergency/alerts"
    const val EMERGENCY_STATUS = "emergency/status"
    const val EMERGENCY_RESPONSE = "emergency/response"
    const val RESPONSE_ACK = "emergency/response/ack"
    
    // Specific incident topics
    fun alertIncident(incidentId: String) = "emergency/alerts/$incidentId"
    const val ALERT_BROADCAST = "emergency/alerts/broadcast"
}
```

### 3. MQTT Client Setup
```kotlin
class MqttService : Service() {
    private lateinit var mqttClient: AndroidXMqttClient
    
    override fun onCreate() {
        super.onCreate()
        
        // Generate unique client ID
        val clientId = "car_crash_client_${System.currentTimeMillis()}_${Random().nextInt(1000)}"
        
        // Initialize MQTT client
        mqttClient = AndroidXMqttClient(applicationContext, MqttConfig.getBrokerUrl(), clientId)
    }
    
    private fun connect() {
        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 20
        }
        
        mqttClient.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.i("MQTT", "Connected to broker")
                subscribeToTopics()
            }
            
            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "Connection failed: ${exception?.message}")
            }
        })
    }
}
```

---

## Two Phone Communication

### 1. Phone 1 (Publisher) - Emergency Detection

```kotlin
class PublisherViewModel : BaseViewModel() {
    
    fun detectEmergency() {
        val sensorData = esp32Service.getCurrentSensorData()
        
        if (sensorData?.impactForce ?: 0f > IMPACT_THRESHOLD) {
            // Crash detected, create emergency alert
            val incidentId = generateIncidentId()
            val alert = EmergencyAlert(
                id = incidentId,
                timestamp = System.currentTimeMillis(),
                location = Location(
                    latitude = sensorData.latitude ?: 0.0,
                    longitude = sensorData.longitude ?: 0.0
                ),
                impactForce = sensorData.impactForce,
                severity = calculateSeverity(sensorData.impactForce)
            )
            
            // Publish emergency alert via MQTT
            publishEmergencyAlert(alert)
            
            // Store incident locally
            saveIncident(alert)
        }
    }
    
    private fun publishEmergencyAlert(alert: EmergencyAlert) {
        val topic = MqttTopics.alertIncident(alert.id)
        val payload = Gson().toJson(alert)
        
        MqttService.publish(topic, payload, 1, false)
        Log.i("Publisher", "Emergency alert published: $topic")
    }
}
```

### 2. Phone 2 (Subscriber) - Alert Reception

```kotlin
class SubscriberViewModel : BaseViewModel() {
    
    init {
        // Subscribe to emergency topics
        subscribeToEmergencyTopics()
    }
    
    private fun subscribeToEmergencyTopics() {
        val topics = listOf(
            MqttTopics.ALERT_BROADCAST,
            MqttTopics.EMERGENCY_ALERTS + "/+",
            MqttTopics.EMERGENCY_STATUS + "/+"
        )
        
        MqttService.subscribeToTopics(topics)
    }
    
    // Handle incoming emergency alerts
    fun onEmergencyAlertReceived(alert: EmergencyAlert) {
        // Display alert notification
        showEmergencyNotification(alert)
        
        // Update UI with incident details
        _incidents.value = _incidents.value?.toMutableList()?.apply {
            add(alert)
        } ?: listOf(alert)
        
        // Log incident for response coordination
        logIncident(alert)
    }
}
```

### 3. MQTT Message Handling

```kotlin
// MQTT callback for message reception
override fun messageArrived(topic: String?, message: MqttMessage?) {
    Log.d("MQTT", "Message received: $topic -> ${message?.toString()}")
    
    when {
        topic?.startsWith(MqttTopics.EMERGENCY_ALERTS) == true -> {
            val alertJson = message.toString()
            val alert = Gson().fromJson(alertJson, EmergencyAlert::class.java)
            
            // Broadcast to app components
            val intent = Intent("com.example.cc.EMERGENCY_ALERT_RECEIVED")
            intent.putExtra("alert_json", alertJson)
            sendBroadcast(intent)
        }
        
        topic?.startsWith(MqttTopics.EMERGENCY_RESPONSE) == true -> {
            val responseJson = message.toString()
            val response = Gson().fromJson(responseJson, EmergencyResponse::class.java)
            
            // Handle response coordination
            handleEmergencyResponse(response)
        }
    }
}
```

---

## Testing and Troubleshooting

### 1. BLE Connection Testing
```bash
# Test ESP32 BLE visibility
# Use nRF Connect app or similar BLE scanner
# Look for device named "ESP32_CarCrash"

# Test data transmission
# Monitor serial output on ESP32
# Check Android logcat for BLE data reception
```

### 2. MQTT Communication Testing
```bash
# Test MQTT broker connectivity
mosquitto_pub -h 192.168.1.100 -t "test/topic" -m "Hello World"

# Test MQTT subscription
mosquitto_sub -h 192.168.1.100 -t "emergency/+/+" -v

# Monitor MQTT traffic
mosquitto_sub -h 192.168.1.100 -t "#" -v
```

### 3. Common Issues and Solutions

**BLE Connection Issues:**
- **Device not found**: Check ESP32 Bluetooth initialization
- **Connection fails**: Verify UUIDs match between ESP32 and Android app
- **Data not received**: Check characteristic notification setup

**MQTT Connection Issues:**
- **Connection refused**: Verify broker IP and port
- **Authentication failed**: Check username/password if enabled
- **Messages not received**: Verify topic subscriptions

**Network Issues:**
- **Phones can't communicate**: Ensure both on same WiFi network
- **Broker unreachable**: Check firewall settings and port 1883

---

## Security Considerations

### 1. BLE Security
```kotlin
// Enable BLE pairing and encryption
private fun enableBLESecurity(gatt: BluetoothGatt) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        gatt.setPreferredPhy(
            BluetoothDevice.PHY_LE_1M,
            BluetoothDevice.PHY_LE_1M,
            BluetoothDevice.PHY_OPTION_NO_PREFERRED
        )
    }
}
```

### 2. MQTT Security
```kotlin
// Enable MQTT authentication
val options = MqttConnectOptions().apply {
    userName = "username"
    password = "password".toCharArray()
    isCleanSession = true
}

// Use SSL/TLS for production
const val BROKER_URL_SSL = "ssl://192.168.1.100:8883"
```

### 3. Data Encryption
```kotlin
// Encrypt sensitive data before transmission
private fun encryptData(data: String): String {
    // Implement encryption algorithm
    return encryptedData
}
```

---

## Advanced Configuration

### 1. Custom MQTT Topics
```kotlin
// Define custom topic patterns
object CustomMqttTopics {
    const val VEHICLE_STATUS = "vehicle/status"
    const val DRIVER_ALERTS = "driver/alerts"
    const val MAINTENANCE = "vehicle/maintenance"
    
    fun vehicleStatus(vehicleId: String) = "vehicle/status/$vehicleId"
    fun driverAlert(driverId: String) = "driver/alerts/$driverId"
}
```

### 2. Quality of Service (QoS) Configuration
```kotlin
// Use different QoS levels for different message types
fun publishWithQoS(topic: String, payload: String, messageType: MessageType) {
    val qos = when (messageType) {
        MessageType.EMERGENCY -> 2  // At most once
        MessageType.STATUS -> 1     // At least once
        MessageType.LOG -> 0        // Exactly once
    }
    
    MqttService.publish(topic, payload, qos, false)
}
```

### 3. Message Persistence
```kotlin
// Store messages locally for offline scenarios
class MessagePersistence {
    fun storeMessage(topic: String, payload: String) {
        // Save to local database
        database.messageDao().insert(Message(topic, payload, System.currentTimeMillis()))
    }
    
    fun retryStoredMessages() {
        val messages = database.messageDao().getPendingMessages()
        messages.forEach { message ->
            MqttService.publish(message.topic, message.payload, 1, false)
        }
    }
}
```

---

## Summary

This guide provides a comprehensive approach to:

1. **Connect ESP32 to Android app via BLE** for real-time sensor data transmission
2. **Establish MQTT communication** between two phones for emergency coordination
3. **Implement crash detection** using accelerometer data from ESP32
4. **Coordinate emergency responses** through real-time messaging

**Key Benefits:**
- **Real-time communication** between emergency responders
- **Reliable data transmission** via BLE and MQTT
- **Scalable architecture** supporting multiple devices
- **Offline capability** with local data storage

**Next Steps:**
1. Upload ESP32 code to your device
2. Configure MQTT broker on your network
3. Install the app on both phones
4. Test BLE connection and MQTT communication
5. Deploy in your vehicle for crash detection

For additional support or customization, refer to the app's source code and documentation.
