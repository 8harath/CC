# Phase 2: MQTT Communication Core - Implementation Plan

## 🎯 Phase 2 Objectives
- Implement reliable MQTT client functionality
- Create robust connection management system
- Establish message processing framework
- Build topic management and subscription system

## 📋 Implementation Checklist

### 1. MQTT Dependencies & Setup
- [ ] Add Eclipse Paho MQTT Android library
- [ ] Configure MQTT broker settings (your Mosquitto)
- [ ] Set up SSL/TLS support for secure communications
- [ ] Create MQTT service for background communication

### 2. Connection Management
- [ ] Implement connection state machine (connecting, connected, disconnected)
- [ ] Create automatic reconnection with exponential backoff
- [ ] Add network change listeners for automatic reconnection
- [ ] Implement connection quality monitoring
- [ ] Add connection status UI indicators

### 3. Message Framework
- [ ] Design JSON message schemas for different message types
- [ ] Create message serialization/deserialization utilities
- [ ] Implement message queue for offline scenarios
- [ ] Add message acknowledgment and retry mechanisms
- [ ] Create message validation and error handling

### 4. Topic Management System
- [ ] Design hierarchical topic structure:
  - `emergency/alerts` (for emergency broadcasts)
  - `emergency/status` (for status updates)
  - `emergency/response` (for responder acknowledgments)
- [ ] Implement dynamic subscription management based on user role
- [ ] Create topic filtering and routing mechanisms
- [ ] Add topic validation and sanitization

### 5. Publisher Mode Enhancements
- [ ] Real emergency alert broadcasting via MQTT
- [ ] GPS location integration for incident coordinates
- [ ] Medical profile data inclusion in emergency messages
- [ ] Emergency alert confirmation and cancellation
- [ ] Offline message queuing for when MQTT is unavailable

### 6. Subscriber Mode Enhancements
- [ ] Real-time emergency alert reception
- [ ] Alert notification system with sound/vibration
- [ ] Incident detail display with victim information
- [ ] Response acknowledgment system
- [ ] Alert history and management

## 🔧 Technical Implementation

### MQTT Configuration
```kotlin
// MQTT Broker Settings (your Mosquitto)
val brokerUrl = "tcp://192.168.1.100:1883"  // Your laptop's IP
val clientId = "android_client_${System.currentTimeMillis()}"
val username = "android_user"  // If authentication is enabled
val password = "android_pass"  // If authentication is enabled
```

### Message Schemas
```json
// Emergency Alert Message
{
  "type": "emergency_alert",
  "incidentId": "unique_incident_id",
  "victimId": "user_id",
  "victimName": "John Doe",
  "location": {
    "latitude": 40.7128,
    "longitude": -74.0060
  },
  "timestamp": 1640995200000,
  "severity": "HIGH",
  "medicalInfo": {
    "bloodType": "O+",
    "allergies": ["penicillin"],
    "medications": ["insulin"]
  }
}

// Response Message
{
  "type": "response_ack",
  "incidentId": "unique_incident_id",
  "responderId": "responder_user_id",
  "responderName": "Emergency Team",
  "status": "RESPONDING",
  "eta": 300,  // seconds
  "timestamp": 1640995200000
}
```

### Topic Structure
```
emergency/
├── alerts/           # Emergency broadcasts
│   ├── +/           # Individual incidents
│   └── broadcast    # General alerts
├── status/          # Status updates
│   ├── +/           # Individual status
│   └── system       # System status
└── response/        # Responder acknowledgments
    ├── +/           # Individual responses
    └── broadcast    # General responses
```

## 🚀 Integration with Your Mosquitto Broker

### Broker Setup Verification
1. **Check Mosquitto Status**:
   ```bash
   # Windows
   netstat -an | findstr 1883
   
   # Or check Mosquitto service
   sc query mosquitto
   ```

2. **Test MQTT Connection**:
   ```bash
   # Install MQTT client for testing
   mosquitto_pub -h localhost -t test/topic -m "Hello MQTT"
   mosquitto_sub -h localhost -t test/topic
   ```

3. **Network Configuration**:
   - Ensure Mosquitto listens on all interfaces: `0.0.0.0:1883`
   - Configure firewall to allow port 1883
   - Get your laptop's IP address for Android connection

### Android Network Configuration
```kotlin
// Get laptop's IP address (usually 192.168.x.x)
val brokerUrl = "tcp://YOUR_LAPTOP_IP:1883"

// For local testing, you can use:
val brokerUrl = "tcp://10.0.2.2:1883"  // Android emulator to localhost
val brokerUrl = "tcp://192.168.1.100:1883"  // Physical device to laptop
```

## 📱 UI Enhancements for Phase 2

### Publisher Mode
- [ ] Connection status indicator
- [ ] GPS location display
- [ ] Emergency alert history
- [ ] Medical profile quick edit
- [ ] Alert confirmation dialog

### Subscriber Mode
- [ ] Real-time alert dashboard
- [ ] Alert notification system
- [ ] Incident detail view
- [ ] Response management interface
- [ ] Connection status monitoring

## 🔒 Security Considerations

### MQTT Security
- [ ] Username/password authentication
- [ ] SSL/TLS encryption
- [ ] Topic access control
- [ ] Message validation and sanitization

### Data Privacy
- [ ] Medical data encryption
- [ ] Location data protection
- [ ] Secure message transmission
- [ ] User consent management

## 🧪 Testing Strategy

### Unit Tests
- [ ] MQTT connection management
- [ ] Message serialization/deserialization
- [ ] Topic management
- [ ] Error handling

### Integration Tests
- [ ] End-to-end MQTT communication
- [ ] Publisher to Subscriber message flow
- [ ] Network interruption handling
- [ ] Reconnection scenarios

### Manual Testing
- [ ] Real device testing with Mosquitto
- [ ] Multiple device scenarios
- [ ] Network condition testing
- [ ] Emergency scenario simulation

## 📊 Success Criteria

- [ ] MQTT client connects reliably to Mosquitto broker
- [ ] Messages are sent and received correctly between devices
- [ ] Auto-reconnection works after network interruptions
- [ ] Connection status is accurately displayed to users
- [ ] Emergency alerts include all required information
- [ ] Responders can acknowledge and respond to alerts
- [ ] System works reliably for extended periods

## 🎯 Next Steps After Phase 2

### Phase 3: ESP32 Integration
- Bluetooth Classic and BLE communication
- WiFi direct fallback
- Sensor data parsing and integration
- Device discovery and pairing

### Phase 4: Advanced Features
- GPS location services
- Medical profile management UI
- Emergency contact system
- Real-time incident monitoring

---

**Ready to Start Phase 2**: Once Phase 1 is running, we can immediately begin MQTT integration with your Mosquitto broker! 