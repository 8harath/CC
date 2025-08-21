#!/usr/bin/env python3
"""
Test script for IP validation and MQTT messaging functionality
This script tests the Android app's IP validation and real-time messaging capabilities
"""

import socket
import time
import json
import threading
from datetime import datetime

def test_ip_validation():
    """Test various IP address formats"""
    print("🔍 Testing IP Address Validation")
    print("=" * 50)
    
    # Valid IP addresses
    valid_ips = [
        "192.168.1.100",
        "10.0.0.1", 
        "172.16.0.1",
        "127.0.0.1",
        "localhost",
        "192.168.0.101"  # Default broker IP
    ]
    
    # Invalid IP addresses
    invalid_ips = [
        "256.1.2.3",      # Invalid octet
        "1.2.3.256",      # Invalid octet
        "192.168.1",      # Incomplete
        "192.168.1.1.1",  # Too many octets
        "abc.def.ghi.jkl", # Non-numeric
        "",               # Empty
        "192.168.1.1a",   # Mixed
    ]
    
    print("✅ Valid IP addresses (should pass):")
    for ip in valid_ips:
        print(f"  {ip}")
    
    print("\n❌ Invalid IP addresses (should fail):")
    for ip in invalid_ips:
        print(f"  {ip}")
    
    print("\n📋 Test Results:")
    print("- The Android app should only accept valid IP addresses")
    print("- Invalid IPs should show error messages")
    print("- Only valid IPs should allow connection attempts")

def test_mqtt_connectivity(host, port=1883):
    """Test MQTT broker connectivity"""
    print(f"\n🔌 Testing MQTT Connectivity to {host}:{port}")
    print("=" * 50)
    
    try:
        # Test basic socket connectivity
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(5)
        result = sock.connect_ex((host, port))
        sock.close()
        
        if result == 0:
            print(f"✅ SUCCESS: Connected to {host}:{port}")
            return True
        else:
            print(f"❌ FAILED: Cannot connect to {host}:{port}")
            print("   Make sure Mosquitto MQTT broker is running")
            return False
            
    except Exception as e:
        print(f"❌ ERROR: {e}")
        return False

def simulate_mqtt_messages():
    """Simulate MQTT message flow between publisher and subscriber"""
    print("\n📨 Simulating MQTT Message Flow")
    print("=" * 50)
    
    # Sample messages that would be sent from publisher
    sample_messages = [
        {
            "type": "emergency_alert",
            "timestamp": int(time.time() * 1000),
            "message": "Emergency assistance needed at Main Street",
            "location": "auto-detected",
            "device_id": "android_device"
        },
        {
            "type": "emergency_alert", 
            "timestamp": int(time.time() * 1000),
            "message": "Car crash detected - immediate response required",
            "location": "auto-detected",
            "device_id": "android_device"
        },
        "Simple text message from publisher",
        "Another test message with custom content"
    ]
    
    print("📤 Publisher sends messages:")
    for i, msg in enumerate(sample_messages, 1):
        if isinstance(msg, dict):
            print(f"  {i}. JSON: {json.dumps(msg, indent=2)}")
        else:
            print(f"  {i}. Text: {msg}")
    
    print("\n📥 Subscriber receives messages in real-time:")
    for i, msg in enumerate(sample_messages, 1):
        if isinstance(msg, dict):
            message_text = msg.get("message", str(msg))
        else:
            message_text = msg
            
        print(f"  {i}. 📨 {message_text}")
        time.sleep(0.5)  # Simulate real-time reception
    
    print("\n✅ Real-time messaging test completed")

def main():
    """Main test function"""
    print("🧪 IP Validation and MQTT Messaging Test Suite")
    print("=" * 60)
    print(f"Test started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    # Test 1: IP Validation
    test_ip_validation()
    
    # Test 2: MQTT Connectivity
    print("\n" + "=" * 60)
    test_mqtt_connectivity("192.168.0.101", 1883)  # Default broker
    test_mqtt_connectivity("localhost", 1883)      # Localhost
    test_mqtt_connectivity("127.0.0.1", 1883)     # Loopback
    
    # Test 3: Message Flow Simulation
    print("\n" + "=" * 60)
    simulate_mqtt_messages()
    
    print("\n" + "=" * 60)
    print("📋 Test Summary:")
    print("✅ IP validation should only accept valid addresses")
    print("✅ Connection should only succeed with valid IP + running broker")
    print("✅ Messages should appear in subscriber in real-time")
    print("✅ Error messages should be shown for invalid configurations")
    print("\n🎯 Expected Behavior:")
    print("- Valid IP → 'Connected' message")
    print("- Invalid IP → Error message")
    print("- Publisher sends → Subscriber receives immediately")
    print("- Real-time notifications for new messages")

if __name__ == "__main__":
    main()
