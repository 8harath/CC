#!/bin/bash

echo "========================================"
echo "Starting Mosquitto MQTT Broker"
echo "========================================"
echo

# Check if Mosquitto is installed
if ! command -v mosquitto &> /dev/null; then
    echo "❌ Mosquitto not found"
    echo "Please install Mosquitto first:"
    echo ""
    echo "For Ubuntu/Debian:"
    echo "  sudo apt update && sudo apt install mosquitto mosquitto-clients"
    echo ""
    echo "For macOS:"
    echo "  brew install mosquitto"
    echo ""
    echo "For other systems, visit: https://mosquitto.org/download/"
    exit 1
fi

echo "✅ Mosquitto found"
echo

# Create data directory if it doesn't exist
mkdir -p mosquitto_data

# Start Mosquitto broker
echo "🚀 Starting Mosquitto broker on port 1883..."
echo "📁 Config file: mosquitto_local.conf"
echo "📁 Data directory: mosquitto_data"
echo "📁 Log file: mosquitto_local.log"
echo
echo "Press Ctrl+C to stop the broker"
echo

# Start the broker with our configuration
mosquitto -c mosquitto_local.conf -v

echo
echo "🔌 Mosquitto broker stopped"
