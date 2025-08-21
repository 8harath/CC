#!/usr/bin/env python3
"""
Simple MQTT Test Script
Tests the exact publisher → broker → subscriber flow used by the Android app
"""

import time
import sys
import threading
from datetime import datetime

try:
    import paho.mqtt.client as mqtt
    MQTT_AVAILABLE = True
except ImportError:
    MQTT_AVAILABLE = False
    print("❌ paho-mqtt not installed. Install with: pip install paho-mqtt")
    sys.exit(1)

class SimpleMQTTTest:
    def __init__(self, broker_ip: str, broker_port: int = 1883):
        self.broker_ip = broker_ip
        self.broker_port = broker_port
        self.publisher_client = None
        self.subscriber_client = None
        self.received_messages = []
        self.test_completed = False
        
    def log(self, message: str):
        timestamp = datetime.now().strftime("%H:%M:%S.%f")[:-3]
        print(f"[{timestamp}] {message}")
    
    def on_publisher_connect(self, client, userdata, flags, rc):
        if rc == 0:
            self.log("✅ Publisher connected successfully")
        else:
            self.log(f"❌ Publisher connection failed with code {rc}")
    
    def on_subscriber_connect(self, client, userdata, flags, rc):
        if rc == 0:
            self.log("✅ Subscriber connected successfully")
            # Subscribe to all emergency topics
            topics = [
                "emergency/alerts/#",
                "emergency/test/#", 
                "emergency/custom/#",
                "emergency/response/#",
                "emergency/status/#",
                "emergency/#"
            ]
            for topic in topics:
                client.subscribe(topic, 1)
                self.log(f"📡 Subscribed to: {topic}")
        else:
            self.log(f"❌ Subscriber connection failed with code {rc}")
    
    def on_message_received(self, client, userdata, msg):
        received_msg = {
            "topic": msg.topic,
            "payload": msg.payload.decode('utf-8'),
            "qos": msg.qos,
            "timestamp": datetime.now().isoformat()
        }
        self.received_messages.append(received_msg)
        self.log(f"📨 Message received: {msg.topic} -> {msg.payload.decode('utf-8')}")
    
    def on_publish_success(self, client, userdata, mid):
        self.log(f"📤 Message published successfully (MID: {mid})")
    
    def on_publish_failure(self, client, userdata, mid):
        self.log(f"❌ Message publish failed (MID: {mid})")
    
    def run_test(self):
        """Run the complete publisher → broker → subscriber test"""
        self.log("🚀 Starting Simple MQTT Test")
        self.log("=" * 50)
        
        try:
            # Create publisher client
            self.publisher_client = mqtt.Client()
            self.publisher_client.on_connect = self.on_publisher_connect
            self.publisher_client.on_publish = self.on_publish_success
            
            # Create subscriber client  
            self.subscriber_client = mqtt.Client()
            self.subscriber_client.on_connect = self.on_subscriber_connect
            self.subscriber_client.on_message = self.on_message_received
            
            # Connect both clients
            self.log(f"🔌 Connecting to broker: {self.broker_ip}:{self.broker_port}")
            
            self.publisher_client.connect(self.broker_ip, self.broker_port, 10)
            self.subscriber_client.connect(self.broker_ip, self.broker_port, 10)
            
            # Start the client loops
            self.publisher_client.loop_start()
            self.subscriber_client.loop_start()
            
            # Wait for connections
            time.sleep(3)
            
            # Test messages to send
            test_messages = [
                {
                    "topic": "emergency/alerts/broadcast",
                    "payload": "Emergency alert test message",
                    "description": "Emergency alert"
                },
                {
                    "topic": "emergency/test/connection", 
                    "payload": "Connection test message",
                    "description": "Connection test"
                },
                {
                    "topic": "emergency/custom/message",
                    "payload": "Custom message test",
                    "description": "Custom message"
                },
                {
                    "topic": "emergency/test/simple",
                    "payload": "hi",
                    "description": "Simple test (like your app)"
                }
            ]
            
            # Send test messages
            for i, test_msg in enumerate(test_messages, 1):
                self.log(f"\n📤 Test {i}: Sending {test_msg['description']}")
                self.log(f"   Topic: {test_msg['topic']}")
                self.log(f"   Payload: {test_msg['payload']}")
                
                # Publish message
                result = self.publisher_client.publish(
                    test_msg['topic'], 
                    test_msg['payload'], 
                    qos=1, 
                    retain=False
                )
                
                # Wait for message delivery
                time.sleep(2)
                
                # Check if message was received
                received = False
                for received_msg in self.received_messages:
                    if (received_msg['topic'] == test_msg['topic'] and 
                        received_msg['payload'] == test_msg['payload']):
                        received = True
                        break
                
                if received:
                    self.log(f"   ✅ Message received by subscriber")
                else:
                    self.log(f"   ❌ Message NOT received by subscriber")
            
            # Summary
            self.log("\n" + "=" * 50)
            self.log("📊 TEST SUMMARY")
            self.log("=" * 50)
            
            total_sent = len(test_messages)
            total_received = len(self.received_messages)
            
            self.log(f"Messages sent: {total_sent}")
            self.log(f"Messages received: {total_received}")
            
            if total_received == total_sent:
                self.log("✅ ALL MESSAGES DELIVERED SUCCESSFULLY!")
                self.log("🎉 Your MQTT setup is working correctly")
            elif total_received > 0:
                self.log(f"⚠️  PARTIAL SUCCESS: {total_received}/{total_sent} messages delivered")
                self.log("🔧 Some messages are getting through, but there may be topic subscription issues")
            else:
                self.log("❌ NO MESSAGES DELIVERED")
                self.log("🔧 There's a fundamental issue with your MQTT setup")
            
            # Show received messages
            if self.received_messages:
                self.log("\n📨 RECEIVED MESSAGES:")
                for i, msg in enumerate(self.received_messages, 1):
                    self.log(f"   {i}. {msg['topic']} -> {msg['payload']}")
            
        except Exception as e:
            self.log(f"❌ Test failed with exception: {e}")
        
        finally:
            # Cleanup
            if self.publisher_client:
                self.publisher_client.disconnect()
                self.publisher_client.loop_stop()
            if self.subscriber_client:
                self.subscriber_client.disconnect()
                self.subscriber_client.loop_stop()
            
            self.log("🔌 Disconnected from broker")

def main():
    """Main function"""
    if len(sys.argv) < 2:
        print("Usage: python test_mqtt_simple.py <broker_ip> [broker_port]")
        print("Example: python test_mqtt_simple.py 192.168.1.100 1883")
        sys.exit(1)
    
    broker_ip = sys.argv[1]
    broker_port = int(sys.argv[2]) if len(sys.argv) > 2 else 1883
    
    print(f"🔍 Testing MQTT communication with broker: {broker_ip}:{broker_port}")
    print("This test simulates the exact flow used by your Android app")
    print()
    
    test = SimpleMQTTTest(broker_ip, broker_port)
    test.run_test()
    
    print("\n💡 If this test works but your Android app doesn't:")
    print("   1. Check that both phones are using the same broker IP")
    print("   2. Verify topic subscriptions in the Android app logs")
    print("   3. Ensure both apps are connected before sending messages")
    print("   4. Check for client ID conflicts")

if __name__ == "__main__":
    main()
