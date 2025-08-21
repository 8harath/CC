#!/bin/bash

echo "========================================"
echo "Local MQTT Setup for Car Crash Detection"
echo "========================================"
echo

# Check if Mosquitto is installed
if ! command -v mosquitto &> /dev/null; then
    echo "ERROR: Mosquitto not found. Please install Mosquitto first."
    echo "Linux: sudo apt install mosquitto mosquitto-clients"
    echo "macOS: brew install mosquitto"
    exit 1
fi

echo "Mosquitto found. Checking if service is running..."

# Check if Mosquitto is running on port 1883
if netstat -an 2>/dev/null | grep -q ":1883 "; then
    echo "✅ Mosquitto is running on port 1883"
elif ss -tuln 2>/dev/null | grep -q ":1883 "; then
    echo "✅ Mosquitto is running on port 1883"
else
    echo "⚠️  Mosquitto not running on port 1883"
    echo "Starting Mosquitto service..."
    
    # Try to start Mosquitto service
    if command -v systemctl &> /dev/null; then
        sudo systemctl start mosquitto
        if [ $? -eq 0 ]; then
            echo "✅ Mosquitto service started successfully"
        else
            echo "❌ Failed to start Mosquitto service"
            echo "Please start it manually: sudo systemctl start mosquitto"
        fi
    elif command -v brew &> /dev/null; then
        brew services start mosquitto
        if [ $? -eq 0 ]; then
            echo "✅ Mosquitto service started successfully"
        else
            echo "❌ Failed to start Mosquitto service"
            echo "Please start it manually: brew services start mosquitto"
        fi
    else
        echo "❌ Could not start Mosquitto service automatically"
        echo "Please start it manually"
    fi
fi

echo
echo "Getting your IP address..."

# Get IP address
if command -v ip &> /dev/null; then
    IP=$(ip route get 1.1.1.1 | awk '{print $7}' | head -n1)
elif command -v ifconfig &> /dev/null; then
    IP=$(ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print $2}' | head -n1)
else
    IP="unknown"
fi

echo "Your IP address: $IP"

echo
echo "Testing MQTT connectivity..."
echo "Publishing test message..."

# Test MQTT connectivity
mosquitto_pub -h localhost -t "test/connection" -m "Hello from setup script!" -q 1
if [ $? -eq 0 ]; then
    echo "✅ MQTT publish test successful"
else
    echo "❌ MQTT publish test failed"
fi

echo
echo "========================================"
echo "Setup Summary:"
echo "========================================"
echo "Broker IP: $IP"
echo "Broker Port: 1883"
echo "Status: Mosquitto running"
echo
echo "Next steps:"
echo "1. Configure Smartphone A (Publisher) with IP: $IP"
echo "2. Configure Smartphone B (Subscriber) with IP: $IP"
echo "3. Test communication between phones"
echo
echo "For detailed instructions, see LOCAL_MQTT_SETUP_GUIDE.md"
echo "========================================"
