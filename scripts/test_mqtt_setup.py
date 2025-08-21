#!/usr/bin/env python3
"""
MQTT Setup Test Script
Tests MQTT broker connectivity and helps with Android app setup
"""

import socket
import sys
import time
import subprocess
import platform
from datetime import datetime

def get_local_ip():
    """Get the local IP address of this machine"""
    try:
        # Create a socket to get local IP
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        local_ip = s.getsockname()[0]
        s.close()
        return local_ip
    except Exception as e:
        print(f"❌ Error getting local IP: {e}")
        return None

def test_port_connectivity(host, port, timeout=5):
    """Test if a port is open and accessible"""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(timeout)
        result = sock.connect_ex((host, port))
        sock.close()
        return result == 0
    except Exception as e:
        print(f"❌ Error testing port {port} on {host}: {e}")
        return False

def test_mqtt_broker(host, port=1883):
    """Test MQTT broker connectivity"""
    print(f"🔍 Testing MQTT broker at {host}:{port}")
    print("=" * 50)
    
    # Test 1: Basic socket connectivity
    print("1. Testing socket connectivity...")
    if test_port_connectivity(host, port):
        print("   ✅ Port is open and accessible")
    else:
        print("   ❌ Port is not accessible")
        print("   💡 Make sure Mosquitto is running and port 1883 is open")
        return False
    
    # Test 2: Try to connect with a simple MQTT client
    print("\n2. Testing MQTT protocol...")
    try:
        import paho.mqtt.client as mqtt
        
        def on_connect(client, userdata, flags, rc):
            if rc == 0:
                print("   ✅ MQTT connection successful")
                client.disconnect()
            else:
                print(f"   ❌ MQTT connection failed with code {rc}")
        
        def on_disconnect(client, userdata, rc):
            print("   📡 MQTT client disconnected")
        
        client = mqtt.Client()
        client.on_connect = on_connect
        client.on_disconnect = on_disconnect
        
        client.connect(host, port, 10)
        client.loop_start()
        time.sleep(2)
        client.loop_stop()
        
    except ImportError:
        print("   ⚠️  paho-mqtt not installed, skipping MQTT protocol test")
        print("   💡 Install with: pip install paho-mqtt")
    except Exception as e:
        print(f"   ❌ MQTT protocol test failed: {e}")
        return False
    
    return True

def check_mosquitto_installation():
    """Check if Mosquitto is installed and running"""
    print("🔍 Checking Mosquitto installation...")
    print("=" * 50)
    
    # Check if mosquitto is installed
    try:
        result = subprocess.run(['mosquitto', '--help'], 
                              capture_output=True, text=True, timeout=5)
        if result.returncode == 0:
            print("✅ Mosquitto is installed")
        else:
            print("❌ Mosquitto is not properly installed")
            return False
    except FileNotFoundError:
        print("❌ Mosquitto is not installed")
        print("💡 Install Mosquitto:")
        if platform.system() == "Windows":
            print("   Download from: https://mosquitto.org/download/")
        elif platform.system() == "Darwin":  # macOS
            print("   brew install mosquitto")
        else:  # Linux
            print("   sudo apt-get install mosquitto mosquitto-clients")
        return False
    except Exception as e:
        print(f"❌ Error checking Mosquitto: {e}")
        return False
    
    # Check if mosquitto is running
    try:
        result = subprocess.run(['pgrep', 'mosquitto'], 
                              capture_output=True, text=True, timeout=5)
        if result.returncode == 0:
            print("✅ Mosquitto is running")
            return True
        else:
            print("⚠️  Mosquitto is not running")
            print("💡 Start Mosquitto with: mosquitto -p 1883")
            return False
    except Exception as e:
        print(f"❌ Error checking if Mosquitto is running: {e}")
        return False

def provide_setup_instructions():
    """Provide setup instructions for the Android app"""
    print("\n📱 Android App Setup Instructions")
    print("=" * 50)
    
    local_ip = get_local_ip()
    if local_ip:
        print(f"1. Your laptop's IP address: {local_ip}")
        print(f"2. In the Android app, enter:")
        print(f"   - Broker IP: {local_ip}")
        print(f"   - Port: 1883")
        print(f"3. Test the connection in the app")
        print(f"4. Try sending a test message")
    else:
        print("❌ Could not determine your IP address")
        print("💡 Please find your IP address manually:")
        if platform.system() == "Windows":
            print("   Run: ipconfig")
        else:
            print("   Run: ifconfig or ip addr")

def main():
    """Main function"""
    print("🚀 MQTT Setup Test Script")
    print("=" * 50)
    print(f"Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print()
    
    # Check Mosquitto installation
    if not check_mosquitto_installation():
        print("\n❌ Mosquitto setup incomplete. Please install and start Mosquitto first.")
        return
    
    # Get local IP
    local_ip = get_local_ip()
    if not local_ip:
        print("\n❌ Could not determine local IP address")
        return
    
    print(f"\n📍 Your local IP address: {local_ip}")
    
    # Test MQTT broker
    if test_mqtt_broker(local_ip, 1883):
        print("\n✅ MQTT broker is ready!")
        print("🎉 You can now use the Android app to connect to your broker")
    else:
        print("\n❌ MQTT broker test failed")
        print("💡 Please check your Mosquitto configuration")
    
    # Provide setup instructions
    provide_setup_instructions()
    
    print("\n" + "=" * 50)
    print("🔧 Additional Commands:")
    print(f"   Test with mosquitto_pub: mosquitto_pub -h {local_ip} -t 'test/topic' -m 'Hello World'")
    print(f"   Test with mosquitto_sub: mosquitto_sub -h {local_ip} -t 'test/topic'")
    print("   Check Mosquitto logs: tail -f /var/log/mosquitto/mosquitto.log")

if __name__ == "__main__":
    main()
