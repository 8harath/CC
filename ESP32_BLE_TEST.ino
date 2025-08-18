/*
 * ESP32 Car Crash Detection System
 * Bluetooth Classic and BLE Communication with Android App
 * MQTT Integration for Local Testing
 * 
 * Features:
 * - Bluetooth Classic for reliable communication
 * - BLE for low-power operation
 * - Accelerometer-based crash detection
 * - Sensor data transmission to Android app
 * - MQTT integration for local testing
 */

#include "BluetoothSerial.h"
#include "BLEDevice.h"
#include "BLEServer.h"
#include "BLEUtils.h"
#include "BLE2902.h"
#include "Wire.h"
#include "MPU6050.h"
#include <WiFi.h>
#include <PubSubClient.h>

// Check if Bluetooth is available
#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to enable it
#endif

// WiFi Configuration
const char* ssid = "YOUR_WIFI_SSID";  // Replace with your WiFi SSID
const char* password = "YOUR_WIFI_PASSWORD";  // Replace with your WiFi password

// MQTT Configuration
const char* mqtt_server = "192.168.1.100";  // Replace with your local Mosquitto broker IP
const int mqtt_port = 1883;
const char* mqtt_client_id = "ESP32_CrashDetector";
const char* mqtt_username = "";  // Leave empty if no authentication
const char* mqtt_password = "";  // Leave empty if no authentication

// MQTT Topics
const char* mqtt_topic_sensor = "esp32/sensor_data";
const char* mqtt_topic_crash = "esp32/crash_alert";
const char* mqtt_topic_status = "esp32/status";
const char* mqtt_topic_test = "esp32/test";

// WiFi and MQTT objects
WiFiClient espClient;
PubSubClient client(espClient);

// Forward declarations
void handleCommand(String command);
void readSensorData();
void detectCrash();
void transmitSensorData();
String createSensorDataString();
String createCrashAlert();
void calibrateSensors();
void setupWiFi();
void setupMQTT();
void reconnectMQTT();
void publishMQTTMessage(const char* topic, const char* message);

// Bluetooth Classic
BluetoothSerial SerialBT;

// BLE
BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;

// BLE Service and Characteristic UUIDs
#define SERVICE_UUID        "0000ffe0-0000-1000-8000-00805f9b34fb"
#define CHARACTERISTIC_UUID "0000ffe1-0000-1000-8000-00805f9b34fb"

// MPU6050 Accelerometer
MPU6050 mpu;

// Crash detection parameters
const float IMPACT_THRESHOLD = 5.0;  // Adjust based on testing
const int SAMPLE_RATE = 100;         // Hz
const int BUFFER_SIZE = 10;          // Number of samples to average

// Sensor data
float accelX, accelY, accelZ;
float impactForce = 0.0;
float accelBuffer[BUFFER_SIZE];
int bufferIndex = 0;

// Timing
unsigned long lastSensorRead = 0;
unsigned long lastDataTransmission = 0;
const unsigned long TRANSMISSION_INTERVAL = 100; // ms

// Device name
const char* DEVICE_NAME = "ESP32_CrashDetector";

// BLE Callback
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("BLE Device connected");
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println("BLE Device disconnected");
    }
};

// BLE Characteristic Callback
class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      // Get the value and convert to Arduino String
      String rxValue = String(pCharacteristic->getValue().c_str());
      if (rxValue.length() > 0) {
        Serial.println("Received Value: " + rxValue);
        handleCommand(rxValue);
      }
    }
};

void setup() {
  Serial.begin(115200);
  Serial.println("ESP32 Car Crash Detection System Starting...");
  
  // Initialize MPU6050
  Wire.begin();
  mpu.initialize();
  
  if (mpu.testConnection()) {
    Serial.println("MPU6050 connection successful");
  } else {
    Serial.println("MPU6050 connection failed");
  }
  
  // Setup WiFi and MQTT
  setupWiFi();
  setupMQTT();
  
  // Initialize Bluetooth Classic
  SerialBT.begin(DEVICE_NAME);
  Serial.println("Bluetooth Classic started");
  Serial.println("Device name: " + String(DEVICE_NAME));
  
  // Initialize BLE
  BLEDevice::init(DEVICE_NAME);
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());
  
  BLEService *pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY |
                      BLECharacteristic::PROPERTY_INDICATE
                    );
                    
  pCharacteristic->setCallbacks(new MyCallbacks());
  pCharacteristic->addDescriptor(new BLE2902());
  
  pService->start();
  
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinPreferred(0x0);
  BLEDevice::startAdvertising();
  
  Serial.println("BLE started");
  Serial.println("Waiting for connections...");
  
  // Initialize sensor buffer
  for (int i = 0; i < BUFFER_SIZE; i++) {
    accelBuffer[i] = 0.0;
  }
  
  // Send initial status via MQTT
  publishMQTTMessage(mqtt_topic_status, "ESP32 Crash Detector Online");
}

void loop() {
  unsigned long currentTime = millis();
  
  // Maintain MQTT connection
  if (!client.connected()) {
    reconnectMQTT();
  }
  client.loop();
  
  // Read sensor data at specified rate
  if (currentTime - lastSensorRead >= (1000 / SAMPLE_RATE)) {
    readSensorData();
    detectCrash();
    lastSensorRead = currentTime;
  }
  
  // Transmit data to connected devices
  if (currentTime - lastDataTransmission >= TRANSMISSION_INTERVAL) {
    transmitSensorData();
    lastDataTransmission = currentTime;
  }
  
  // Handle BLE connections
  if (!deviceConnected && oldDeviceConnected) {
    delay(500); // Give the BLE stack the chance to get things ready
    pServer->startAdvertising(); // Restart advertising
    Serial.println("Start advertising");
    oldDeviceConnected = deviceConnected;
  }
  
  if (deviceConnected && !oldDeviceConnected) {
    oldDeviceConnected = deviceConnected;
  }
  
  // Handle Bluetooth Classic connections
  if (SerialBT.available()) {
    String command = SerialBT.readString();
    handleCommand(command);
  }
  
  delay(10); // Small delay to prevent watchdog issues
}

void readSensorData() {
  // Read accelerometer data
  int16_t ax, ay, az;
  mpu.getAcceleration(&ax, &ay, &az);
  
  // Convert to g-force (MPU6050 sensitivity is typically 16384 LSB/g)
  accelX = ax / 16384.0;
  accelY = ay / 16384.0;
  accelZ = az / 16384.0;
  
  // Calculate magnitude for impact detection
  float magnitude = sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);
  
  // Add to buffer for averaging
  accelBuffer[bufferIndex] = magnitude;
  bufferIndex = (bufferIndex + 1) % BUFFER_SIZE;
  
  // Calculate average magnitude
  float avgMagnitude = 0.0;
  for (int i = 0; i < BUFFER_SIZE; i++) {
    avgMagnitude += accelBuffer[i];
  }
  avgMagnitude /= BUFFER_SIZE;
  
  // Calculate impact force (deviation from 1g)
  impactForce = abs(avgMagnitude - 1.0);
}

void detectCrash() {
  if (impactForce > IMPACT_THRESHOLD) {
    Serial.println("CRASH DETECTED! Impact force: " + String(impactForce));
    
    // Send crash alert via both Bluetooth Classic and BLE
    String crashAlert = createCrashAlert();
    
    // Send via Bluetooth Classic
    if (SerialBT.hasClient()) {
      SerialBT.println(crashAlert);
    }
    
    // Send via BLE
    if (deviceConnected) {
      pCharacteristic->setValue(crashAlert.c_str());
      pCharacteristic->notify();
    }
    
    // Send via MQTT
    publishMQTTMessage(mqtt_topic_crash, crashAlert.c_str());
    
    // Flash LED or other indicator
    digitalWrite(2, HIGH);
    delay(100);
    digitalWrite(2, LOW);
  }
}

void transmitSensorData() {
  String sensorData = createSensorDataString();
  
  // Send via Bluetooth Classic
  if (SerialBT.hasClient()) {
    SerialBT.println(sensorData);
  }
  
  // Send via BLE
  if (deviceConnected) {
    pCharacteristic->setValue(sensorData.c_str());
    pCharacteristic->notify();
  }
  
  // Send via MQTT (less frequently to avoid flooding)
  static unsigned long lastMQTTSensorPublish = 0;
  if (millis() - lastMQTTSensorPublish >= 5000) { // Publish sensor data every 5 seconds
    publishMQTTMessage(mqtt_topic_sensor, sensorData.c_str());
    lastMQTTSensorPublish = millis();
  }
}

String createSensorDataString() {
  String data = "SENSOR:";
  data += "ACCEL:" + String(accelX, 3) + "," + String(accelY, 3) + "," + String(accelZ, 3);
  data += "|IMPACT:" + String(impactForce, 3);
  data += "|TIME:" + String(millis());
  return data;
}

String createCrashAlert() {
  String alert = "CRASH:";
  alert += "SEVERITY:" + String(impactForce > 10.0 ? "CRITICAL" : "HIGH");
  alert += "|IMPACT:" + String(impactForce, 3);
  alert += "|ACCEL:" + String(accelX, 3) + "," + String(accelY, 3) + "," + String(accelZ, 3);
  alert += "|TIME:" + String(millis());
  return alert;
}

void handleCommand(String command) {
  Serial.println("Received command: " + command);
  
  if (command.startsWith("GET_STATUS")) {
    String status = "STATUS:OK|IMPACT:" + String(impactForce, 3) + "|CONNECTED:true";
    SerialBT.println(status);
  }
  else if (command.startsWith("SET_THRESHOLD:")) {
    float newThreshold = command.substring(14).toFloat();
    if (newThreshold > 0) {
      SerialBT.println("THRESHOLD_SET:" + String(newThreshold));
    }
  }
  else if (command.startsWith("CALIBRATE")) {
    calibrateSensors();
    SerialBT.println("CALIBRATION_COMPLETE");
  }
  else if (command.startsWith("TEST_CRASH")) {
    Serial.println("Simulating crash detection...");
    detectCrash();
  }
}

void calibrateSensors() {
  Serial.println("Calibrating sensors...");
  
  // Reset buffer
  for (int i = 0; i < BUFFER_SIZE; i++) {
    accelBuffer[i] = 0.0;
  }
  bufferIndex = 0;
  
  // Take multiple readings to establish baseline
  for (int i = 0; i < 100; i++) {
    readSensorData();
    delay(10);
  }
  
  Serial.println("Calibration complete");
}

// MQTT Functions
void setupWiFi() {
  Serial.println("Connecting to WiFi...");
  WiFi.begin(ssid, password);
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

void setupMQTT() {
  client.setServer(mqtt_server, mqtt_port);
  Serial.println("MQTT client configured");
}

void reconnectMQTT() {
  // Loop until we're reconnected
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Create a random client ID
    String clientId = String(mqtt_client_id) + "-" + String(random(0xffff), HEX);
    
    // Attempt to connect
    if (client.connect(clientId.c_str(), mqtt_username, mqtt_password)) {
      Serial.println("connected");
      // Once connected, publish an announcement...
      publishMQTTMessage(mqtt_topic_status, "ESP32 Crash Detector Reconnected");
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}

void publishMQTTMessage(const char* topic, const char* message) {
  if (client.connected()) {
    client.publish(topic, message);
    Serial.println("MQTT Published to " + String(topic) + ": " + String(message));
  } else {
    Serial.println("MQTT not connected, cannot publish");
  }
}
