#!/usr/bin/env python3
"""
Simple MQTT Broker Test Script
Tests connection to the MQTT broker at 10.0.0.208:1883
"""

import paho.mqtt.client as mqtt
import time
import sys

# MQTT Broker Configuration
BROKER_IP = "10.0.0.208"
BROKER_PORT = 1883
CLIENT_ID = "test_client_python"

# Test topics
TEST_TOPIC = "emergency/test/python"
CUSTOM_TOPIC = "emergency/custom/python"
ALERT_TOPIC = "emergency/alerts/test"

def on_connect(client, userdata, flags, rc):
    """Callback when connected to MQTT broker"""
    if rc == 0:
        print(f"✅ Connected to MQTT broker at {BROKER_IP}:{BROKER_PORT}")
        print(f"Connection result code: {rc}")
        
        # Subscribe to test topics
        client.subscribe(TEST_TOPIC)
        client.subscribe(CUSTOM_TOPIC)
        client.subscribe(ALERT_TOPIC)
        print(f"📡 Subscribed to topics: {TEST_TOPIC}, {CUSTOM_TOPIC}, {ALERT_TOPIC}")
        
    else:
        print(f"❌ Failed to connect to MQTT broker. Return code: {rc}")
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
        print("🔌 Disconnected from MQTT broker")

def test_mqtt_connection():
    """Test MQTT broker connection and basic functionality"""
    print(f"🔍 Testing MQTT broker connection to {BROKER_IP}:{BROKER_PORT}")
    
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
        test_message1 = f"Python test message at {time.time()}"
        result1 = client.publish(TEST_TOPIC, test_message1, qos=1)
        if result1.rc == mqtt.MQTT_ERR_SUCCESS:
            print(f"✅ Test message 1 published to {TEST_TOPIC}")
        else:
            print(f"❌ Failed to publish test message 1: {result1.rc}")
        
        time.sleep(1)
        
        # Test message 2
        test_message2 = f"Custom message from Python at {time.time()}"
        result2 = client.publish(CUSTOM_TOPIC, test_message2, qos=1)
        if result2.rc == mqtt.MQTT_ERR_SUCCESS:
            print(f"✅ Test message 2 published to {CUSTOM_TOPIC}")
        else:
            print(f"❌ Failed to publish test message 2: {result2.rc}")
        
        time.sleep(1)
        
        # Test message 3 (JSON format)
        import json
        test_alert = {
            "incidentId": f"python_test_{int(time.time())}",
            "victimId": "python_test_user",
            "victimName": "Python Test User",
            "timestamp": int(time.time() * 1000),
            "severity": "TEST",
            "message": "Test alert from Python script"
        }
        test_message3 = json.dumps(test_alert)
        result3 = client.publish(ALERT_TOPIC, test_message3, qos=1)
        if result3.rc == mqtt.MQTT_ERR_SUCCESS:
            print(f"✅ Test alert published to {ALERT_TOPIC}")
        else:
            print(f"❌ Failed to publish test alert: {result3.rc}")
        
        # Wait for any incoming messages
        print("\n⏳ Waiting for incoming messages (10 seconds)...")
        time.sleep(10)
        
        # Disconnect
        print("\n🔌 Disconnecting from MQTT broker...")
        client.loop_stop()
        client.disconnect()
        
        print("✅ MQTT broker test completed successfully!")
        
    except Exception as e:
        print(f"❌ Error during MQTT test: {e}")
        sys.exit(1)

if __name__ == "__main__":
    test_mqtt_connection()
