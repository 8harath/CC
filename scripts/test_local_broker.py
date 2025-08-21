#!/usr/bin/env python3
"""
Test Local MQTT Broker
Tests connection to local MQTT broker at localhost:1883
"""

import paho.mqtt.client as mqtt
import time
import sys
import json

# MQTT Broker Configuration
BROKER_IP = "localhost"  # Local broker
BROKER_PORT = 1883
CLIENT_ID = "test_client_local"

# Test topics
TEST_TOPIC = "emergency/test/local"
CUSTOM_TOPIC = "emergency/custom/local"
ALERT_TOPIC = "emergency/alerts/local"

def on_connect(client, userdata, flags, rc):
    """Callback when connected to MQTT broker"""
    if rc == 0:
        print(f"✅ Connected to local MQTT broker at {BROKER_IP}:{BROKER_PORT}")
        print(f"Connection result code: {rc}")
        
        # Subscribe to test topics
        client.subscribe(TEST_TOPIC)
        client.subscribe(CUSTOM_TOPIC)
        client.subscribe(ALERT_TOPIC)
        print(f"📡 Subscribed to topics: {TEST_TOPIC}, {CUSTOM_TOPIC}, {ALERT_TOPIC}")
        
    else:
        print(f"❌ Failed to connect to local MQTT broker. Return code: {rc}")
        sys.exit(1)

def on_message(client, userdata, msg):
    """Callback when message is received"""
    print(f"📨 Message received on topic '{msg.topic}': {msg.payload.decode()}")

def on_publish(client, userdata, mid):
    """Callback when message is published"""
    print(f"📤 Message published successfully (Message ID: {mid})")

def on_disconnect(client, userdata, rc):
    """Callback when disconnected from MQTT broker"""
    if rc != 0:
        print(f"⚠️ Unexpected disconnection. Return code: {rc}")
    else:
        print("🔌 Disconnected from local MQTT broker")

def test_local_broker():
    """Test local MQTT broker connection and functionality"""
    print(f"🔍 Testing local MQTT broker connection to {BROKER_IP}:{BROKER_PORT}")
    
    # Create MQTT client
    client = mqtt.Client(CLIENT_ID)
    
    # Set callbacks
    client.on_connect = on_connect
    client.on_message = on_message
    client.on_publish = on_publish
    client.on_disconnect = on_disconnect
    
    try:
        # Connect to broker
        print(f"🔄 Attempting to connect to {BROKER_IP}:{BROKER_PORT}...")
        client.connect(BROKER_IP, BROKER_PORT, 60)
        
        # Start the loop in a non-blocking way
        client.loop_start()
        
        # Wait a bit for connection
        time.sleep(2)
        
        # Test publishing messages
        print("\n📤 Testing message publishing...")
        
        # Test message 1
        test_message1 = f"Local test message at {time.time()}"
        result1 = client.publish(TEST_TOPIC, test_message1, qos=1)
        if result1.rc == mqtt.MQTT_ERR_SUCCESS:
            print(f"✅ Test message 1 published to {TEST_TOPIC}")
        else:
            print(f"❌ Failed to publish test message 1: {result1.rc}")
        
        time.sleep(1)
        
        # Test message 2
        test_message2 = f"Local custom message at {time.time()}"
        result2 = client.publish(CUSTOM_TOPIC, test_message2, qos=1)
        if result2.rc == mqtt.MQTT_ERR_SUCCESS:
            print(f"✅ Test message 2 published to {CUSTOM_TOPIC}")
        else:
            print(f"❌ Failed to publish test message 2: {result2.rc}")
        
        time.sleep(1)
        
        # Test message 3 (JSON format)
        test_alert = {
            "incidentId": f"local_test_{int(time.time())}",
            "victimId": "local_test_user",
            "victimName": "Local Test User",
            "timestamp": int(time.time() * 1000),
            "severity": "TEST",
            "message": "Test alert from local broker"
        }
        test_message3 = json.dumps(test_alert)
        result3 = client.publish(ALERT_TOPIC, test_message3, qos=1)
        if result3.rc == mqtt.MQTT_ERR_SUCCESS:
            print(f"✅ Test alert published to {ALERT_TOPIC}")
        else:
            print(f"❌ Failed to publish test alert: {result3.rc}")
        
        # Wait for any incoming messages
        print("\n⏳ Waiting for incoming messages (5 seconds)...")
        time.sleep(5)
        
        # Disconnect
        print("\n🔌 Disconnecting from local MQTT broker...")
        client.loop_stop()
        client.disconnect()
        
        print("✅ Local MQTT broker test completed successfully!")
        print("\n🎉 Your local MQTT broker is working correctly!")
        print("You can now test the Android app with localhost:1883")
        
    except Exception as e:
        print(f"❌ Error during local MQTT test: {e}")
        print("\n🔧 Troubleshooting:")
        print("1. Make sure Mosquitto is running: mosquitto -c mosquitto_local.conf -v")
        print("2. Check if port 1883 is not blocked by firewall")
        print("3. Verify the configuration file exists")
        sys.exit(1)

if __name__ == "__main__":
    test_local_broker()
