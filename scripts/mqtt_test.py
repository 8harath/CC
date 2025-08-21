#!/usr/bin/env python3
"""
MQTT Test Script for ESP32 Car Crash Detection System
This script helps test MQTT communication with the ESP32 device.
"""

import paho.mqtt.client as mqtt
import json
import time
from datetime import datetime

# MQTT Configuration
MQTT_BROKER = "localhost"  # or "192.168.1.100" if running on different machine
MQTT_PORT = 1883
MQTT_CLIENT_ID = "mqtt_test_client"

# Topics to subscribe to
TOPICS = [
    "esp32/sensor_data",
    "esp32/crash_alert", 
    "esp32/status",
    "esp32/test"
]

# Topics to publish to
TEST_TOPIC = "esp32/command"

class MQTTTester:
    def __init__(self):
        self.client = mqtt.Client(MQTT_CLIENT_ID)
        self.client.on_connect = self.on_connect
        self.client.on_message = self.on_message
        self.client.on_disconnect = self.on_disconnect
        
    def on_connect(self, client, userdata, flags, rc):
        print(f"Connected to MQTT broker with result code {rc}")
        if rc == 0:
            print("Successfully connected to MQTT broker!")
            # Subscribe to all topics
            for topic in TOPICS:
                client.subscribe(topic)
                print(f"Subscribed to: {topic}")
        else:
            print(f"Failed to connect to MQTT broker with code: {rc}")
    
    def on_message(self, client, userdata, msg):
        timestamp = datetime.now().strftime("%H:%M:%S")
        print(f"[{timestamp}] {msg.topic}: {msg.payload.decode()}")
    
    def on_disconnect(self, client, userdata, rc):
        print(f"Disconnected from MQTT broker with result code {rc}")
    
    def connect(self):
        try:
            self.client.connect(MQTT_BROKER, MQTT_PORT, 60)
            print(f"Connecting to MQTT broker at {MQTT_BROKER}:{MQTT_PORT}")
        except Exception as e:
            print(f"Failed to connect to MQTT broker: {e}")
            return False
        return True
    
    def publish_test_message(self, message):
        """Publish a test message to the ESP32"""
        result = self.client.publish(TEST_TOPIC, message)
        if result.rc == mqtt.MQTT_ERR_SUCCESS:
            print(f"Published test message: {message}")
        else:
            print(f"Failed to publish message: {result.rc}")
    
    def start(self):
        """Start the MQTT client loop"""
        self.client.loop_start()
    
    def stop(self):
        """Stop the MQTT client loop"""
        self.client.loop_stop()
        self.client.disconnect()

def main():
    print("MQTT Test Script for ESP32 Car Crash Detection System")
    print("=" * 60)
    
    # Create MQTT tester
    tester = MQTTTester()
    
    # Connect to broker
    if not tester.connect():
        print("Failed to connect to MQTT broker. Please check:")
        print("1. Mosquitto broker is running")
        print("2. Broker IP address is correct")
        print("3. Network connectivity")
        return
    
    # Start the client loop
    tester.start()
    
    # Wait a moment for connection
    time.sleep(2)
    
    print("\nMQTT Test Commands:")
    print("1. Send MQTT test message")
    print("2. Send crash test command")
    print("3. Send status request")
    print("4. Exit")
    
    try:
        while True:
            print("\nEnter command (1-4): ", end="")
            choice = input().strip()
            
            if choice == "1":
                test_msg = "MQTT_TEST"
                tester.publish_test_message(test_msg)
                
            elif choice == "2":
                test_msg = "TEST_CRASH"
                tester.publish_test_message(test_msg)
                
            elif choice == "3":
                test_msg = "GET_STATUS"
                tester.publish_test_message(test_msg)
                
            elif choice == "4":
                print("Exiting...")
                break
                
            else:
                print("Invalid choice. Please enter 1-4.")
                
            time.sleep(1)
            
    except KeyboardInterrupt:
        print("\nInterrupted by user")
    
    finally:
        tester.stop()
        print("MQTT test completed")

if __name__ == "__main__":
    main()
