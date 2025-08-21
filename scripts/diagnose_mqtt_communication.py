#!/usr/bin/env python3
"""
MQTT Communication Diagnostic Tool
Comprehensive analysis of publisher → broker → subscriber pipeline
"""

import socket
import sys
import time
import subprocess
import platform
import json
import threading
from datetime import datetime
from typing import Optional, Dict, List

try:
    import paho.mqtt.client as mqtt
    MQTT_AVAILABLE = True
except ImportError:
    MQTT_AVAILABLE = False
    print("⚠️  paho-mqtt not installed. Install with: pip install paho-mqtt")

class MQTTDiagnostic:
    def __init__(self, broker_ip: str, broker_port: int = 1883):
        self.broker_ip = broker_ip
        self.broker_port = broker_port
        self.test_results = {}
        self.received_messages = []
        self.publisher_client = None
        self.subscriber_client = None
        
    def log(self, message: str, level: str = "INFO"):
        timestamp = datetime.now().strftime("%H:%M:%S.%f")[:-3]
        print(f"[{timestamp}] {level}: {message}")
    
    def test_network_connectivity(self) -> bool:
        """Test basic network connectivity to broker"""
        self.log("🔍 Testing network connectivity...")
        
        try:
            # Test 1: Socket connectivity
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(5)
            result = sock.connect_ex((self.broker_ip, self.broker_port))
            sock.close()
            
            if result == 0:
                self.log("✅ Socket connectivity test passed", "SUCCESS")
                self.test_results["network_connectivity"] = True
                return True
            else:
                self.log(f"❌ Socket connectivity test failed (error code: {result})", "ERROR")
                self.test_results["network_connectivity"] = False
                return False
                
        except Exception as e:
            self.log(f"❌ Network connectivity test exception: {e}", "ERROR")
            self.test_results["network_connectivity"] = False
            return False
    
    def test_mqtt_protocol(self) -> bool:
        """Test MQTT protocol connectivity"""
        if not MQTT_AVAILABLE:
            self.log("⚠️  Skipping MQTT protocol test (paho-mqtt not available)", "WARNING")
            return False
            
        self.log("🔍 Testing MQTT protocol connectivity...")
        
        try:
            client = mqtt.Client()
            client.on_connect = lambda client, userdata, flags, rc: self._on_connect(client, userdata, flags, rc)
            client.on_disconnect = lambda client, userdata, rc: self._on_disconnect(client, userdata, rc)
            
            client.connect(self.broker_ip, self.broker_port, 10)
            client.loop_start()
            
            # Wait for connection
            time.sleep(2)
            
            if hasattr(self, '_connection_result'):
                if self._connection_result == 0:
                    self.log("✅ MQTT protocol test passed", "SUCCESS")
                    self.test_results["mqtt_protocol"] = True
                    client.disconnect()
                    client.loop_stop()
                    return True
                else:
                    self.log(f"❌ MQTT protocol test failed (return code: {self._connection_result})", "ERROR")
                    self.test_results["mqtt_protocol"] = False
                    client.disconnect()
                    client.loop_stop()
                    return False
            else:
                self.log("❌ MQTT protocol test timeout", "ERROR")
                self.test_results["mqtt_protocol"] = False
                client.disconnect()
                client.loop_stop()
                return False
                
        except Exception as e:
            self.log(f"❌ MQTT protocol test exception: {e}", "ERROR")
            self.test_results["mqtt_protocol"] = False
            return False
    
    def _on_connect(self, client, userdata, flags, rc):
        self._connection_result = rc
        if rc == 0:
            self.log("MQTT client connected successfully", "DEBUG")
        else:
            self.log(f"MQTT client connection failed with code {rc}", "DEBUG")
    
    def _on_disconnect(self, client, userdata, rc):
        self.log("MQTT client disconnected", "DEBUG")
    
    def test_publisher_subscriber_communication(self) -> bool:
        """Test end-to-end publisher → broker → subscriber communication"""
        if not MQTT_AVAILABLE:
            self.log("⚠️  Skipping publisher-subscriber test (paho-mqtt not available)", "WARNING")
            return False
            
        self.log("🔍 Testing publisher → broker → subscriber communication...")
        
        try:
            # Create test topic and message
            test_topic = "emergency/test/diagnostic"
            test_message = f"Diagnostic test message - {datetime.now().isoformat()}"
            
            # Setup subscriber client
            self.subscriber_client = mqtt.Client()
            self.subscriber_client.on_connect = lambda client, userdata, flags, rc: self._on_subscriber_connect(client, userdata, flags, rc)
            self.subscriber_client.on_message = lambda client, userdata, msg: self._on_message_received(client, userdata, msg)
            
            # Setup publisher client
            self.publisher_client = mqtt.Client()
            self.publisher_client.on_connect = lambda client, userdata, flags, rc: self._on_publisher_connect(client, userdata, flags, rc)
            
            # Connect both clients
            self.subscriber_client.connect(self.broker_ip, self.broker_port, 10)
            self.publisher_client.connect(self.broker_ip, self.broker_port, 10)
            
            self.subscriber_client.loop_start()
            self.publisher_client.loop_start()
            
            # Wait for connections
            time.sleep(3)
            
            # Subscribe to test topic
            self.subscriber_client.subscribe(test_topic, 1)
            time.sleep(1)
            
            # Publish test message
            self.log(f"📤 Publishing test message to {test_topic}: {test_message}")
            self.publisher_client.publish(test_topic, test_message, 1)
            
            # Wait for message delivery
            time.sleep(3)
            
            # Check if message was received
            if self.received_messages:
                received_msg = self.received_messages[0]
                if received_msg["topic"] == test_topic and received_msg["payload"] == test_message:
                    self.log("✅ Publisher → broker → subscriber communication test passed", "SUCCESS")
                    self.test_results["publisher_subscriber_communication"] = True
                    
                    # Cleanup
                    self.subscriber_client.disconnect()
                    self.publisher_client.disconnect()
                    self.subscriber_client.loop_stop()
                    self.publisher_client.loop_stop()
                    return True
                else:
                    self.log(f"❌ Message mismatch. Expected: {test_message}, Received: {received_msg}", "ERROR")
                    self.test_results["publisher_subscriber_communication"] = False
            else:
                self.log("❌ No message received by subscriber", "ERROR")
                self.test_results["publisher_subscriber_communication"] = False
            
            # Cleanup
            self.subscriber_client.disconnect()
            self.publisher_client.disconnect()
            self.subscriber_client.loop_stop()
            self.publisher_client.loop_stop()
            return False
            
        except Exception as e:
            self.log(f"❌ Publisher-subscriber test exception: {e}", "ERROR")
            self.test_results["publisher_subscriber_communication"] = False
            return False
    
    def _on_subscriber_connect(self, client, userdata, flags, rc):
        if rc == 0:
            self.log("Subscriber client connected", "DEBUG")
        else:
            self.log(f"Subscriber client connection failed with code {rc}", "DEBUG")
    
    def _on_publisher_connect(self, client, userdata, flags, rc):
        if rc == 0:
            self.log("Publisher client connected", "DEBUG")
        else:
            self.log(f"Publisher client connection failed with code {rc}", "DEBUG")
    
    def _on_message_received(self, client, userdata, msg):
        received_msg = {
            "topic": msg.topic,
            "payload": msg.payload.decode('utf-8'),
            "qos": msg.qos,
            "timestamp": datetime.now().isoformat()
        }
        self.received_messages.append(received_msg)
        self.log(f"📨 Message received: {received_msg}", "DEBUG")
    
    def test_android_app_topics(self) -> bool:
        """Test the specific topics used by the Android app"""
        if not MQTT_AVAILABLE:
            self.log("⚠️  Skipping Android topics test (paho-mqtt not available)", "WARNING")
            return False
            
        self.log("🔍 Testing Android app specific topics...")
        
        try:
            # Test topics used by the Android app
            test_topics = [
                "emergency/alerts/broadcast",
                "emergency/test/connection",
                "emergency/custom/message"
            ]
            
            client = mqtt.Client()
            client.on_connect = lambda client, userdata, flags, rc: self._on_connect(client, userdata, flags, rc)
            
            client.connect(self.broker_ip, self.broker_port, 10)
            client.loop_start()
            
            time.sleep(2)
            
            if hasattr(self, '_connection_result') and self._connection_result == 0:
                # Subscribe to all test topics
                for topic in test_topics:
                    client.subscribe(topic, 1)
                    self.log(f"✅ Subscribed to {topic}", "SUCCESS")
                
                # Publish test messages
                for topic in test_topics:
                    test_message = f"Android app test - {topic} - {datetime.now().isoformat()}"
                    client.publish(topic, test_message, 1)
                    self.log(f"📤 Published to {topic}: {test_message}", "DEBUG")
                
                time.sleep(2)
                client.disconnect()
                client.loop_stop()
                
                self.log("✅ Android app topics test passed", "SUCCESS")
                self.test_results["android_app_topics"] = True
                return True
            else:
                self.log("❌ Android app topics test failed - could not connect", "ERROR")
                self.test_results["android_app_topics"] = False
                return False
                
        except Exception as e:
            self.log(f"❌ Android app topics test exception: {e}", "ERROR")
            self.test_results["android_app_topics"] = False
            return False
    
    def check_mosquitto_logs(self) -> Dict:
        """Check Mosquitto broker logs for errors"""
        self.log("🔍 Checking Mosquitto broker logs...")
        
        log_info = {
            "mosquitto_running": False,
            "log_file_accessible": False,
            "recent_errors": [],
            "connection_attempts": 0
        }
        
        try:
            # Check if mosquitto is running
            if platform.system() == "Windows":
                result = subprocess.run(['tasklist', '/FI', 'IMAGENAME eq mosquitto.exe'], 
                                      capture_output=True, text=True, timeout=5)
            else:
                result = subprocess.run(['pgrep', 'mosquitto'], 
                                      capture_output=True, text=True, timeout=5)
            
            if result.returncode == 0:
                log_info["mosquitto_running"] = True
                self.log("✅ Mosquitto broker is running", "SUCCESS")
            else:
                self.log("❌ Mosquitto broker is not running", "ERROR")
                
        except Exception as e:
            self.log(f"❌ Error checking Mosquitto process: {e}", "ERROR")
        
        # Try to check log files
        log_paths = [
            "/var/log/mosquitto/mosquitto.log",
            "mosquitto.log",
            "C:\\mosquitto\\mosquitto.log"
        ]
        
        for log_path in log_paths:
            try:
                with open(log_path, 'r') as f:
                    lines = f.readlines()
                    log_info["log_file_accessible"] = True
                    
                    # Check last 50 lines for errors
                    recent_lines = lines[-50:] if len(lines) > 50 else lines
                    for line in recent_lines:
                        if "error" in line.lower() or "failed" in line.lower():
                            log_info["recent_errors"].append(line.strip())
                        if "connection" in line.lower():
                            log_info["connection_attempts"] += 1
                    
                    self.log(f"✅ Found Mosquitto log file: {log_path}", "SUCCESS")
                    break
                    
            except FileNotFoundError:
                continue
            except Exception as e:
                self.log(f"⚠️  Error reading log file {log_path}: {e}", "WARNING")
        
        self.test_results["mosquitto_logs"] = log_info
        return log_info
    
    def generate_diagnostic_report(self) -> str:
        """Generate comprehensive diagnostic report"""
        report = []
        report.append("=" * 80)
        report.append("MQTT COMMUNICATION DIAGNOSTIC REPORT")
        report.append("=" * 80)
        report.append(f"Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        report.append(f"Broker: {self.broker_ip}:{self.broker_port}")
        report.append("")
        
        # Test Results Summary
        report.append("TEST RESULTS SUMMARY:")
        report.append("-" * 40)
        
        for test_name, result in self.test_results.items():
            if isinstance(result, bool):
                status = "✅ PASS" if result else "❌ FAIL"
                report.append(f"{test_name}: {status}")
            elif isinstance(result, dict):
                report.append(f"{test_name}: {result}")
        
        report.append("")
        
        # Detailed Analysis
        report.append("DETAILED ANALYSIS:")
        report.append("-" * 40)
        
        if not self.test_results.get("network_connectivity", False):
            report.append("❌ NETWORK CONNECTIVITY ISSUE")
            report.append("   - Publisher cannot reach the broker")
            report.append("   - Check if broker IP address is correct")
            report.append("   - Check if broker is running on the specified port")
            report.append("   - Check firewall settings")
            report.append("")
        
        if not self.test_results.get("mqtt_protocol", False):
            report.append("❌ MQTT PROTOCOL ISSUE")
            report.append("   - Network connectivity exists but MQTT handshake fails")
            report.append("   - Check if Mosquitto is properly configured")
            report.append("   - Check if port 1883 is dedicated to MQTT")
            report.append("")
        
        if not self.test_results.get("publisher_subscriber_communication", False):
            report.append("❌ END-TO-END COMMUNICATION ISSUE")
            report.append("   - Publisher can connect but subscriber doesn't receive messages")
            report.append("   - Possible causes:")
            report.append("     * Different topic subscriptions")
            report.append("     * QoS level mismatches")
            report.append("     * Message retention settings")
            report.append("     * Client ID conflicts")
            report.append("")
        
        # Recommendations
        report.append("RECOMMENDATIONS:")
        report.append("-" * 40)
        
        if self.test_results.get("network_connectivity", False) and self.test_results.get("mqtt_protocol", False):
            report.append("✅ Network and MQTT protocol are working")
            report.append("🔧 Next steps:")
            report.append("   1. Check Android app topic subscriptions")
            report.append("   2. Verify QoS settings match between publisher and subscriber")
            report.append("   3. Check for client ID conflicts")
            report.append("   4. Test with simple messages first")
        else:
            report.append("🔧 Fix network/MQTT issues first:")
            report.append("   1. Ensure Mosquitto is running: mosquitto -p 1883")
            report.append("   2. Verify broker IP address in Android app")
            report.append("   3. Check firewall allows port 1883")
            report.append("   4. Test with command line tools:")
            report.append(f"      mosquitto_pub -h {self.broker_ip} -t 'test/topic' -m 'test'")
            report.append(f"      mosquitto_sub -h {self.broker_ip} -t 'test/topic'")
        
        report.append("")
        report.append("=" * 80)
        
        return "\n".join(report)
    
    def run_full_diagnostic(self) -> str:
        """Run complete diagnostic suite"""
        self.log("🚀 Starting MQTT Communication Diagnostic", "INFO")
        self.log("=" * 60, "INFO")
        
        # Run all tests
        self.test_network_connectivity()
        time.sleep(1)
        
        self.test_mqtt_protocol()
        time.sleep(1)
        
        self.test_publisher_subscriber_communication()
        time.sleep(1)
        
        self.test_android_app_topics()
        time.sleep(1)
        
        self.check_mosquitto_logs()
        
        # Generate and return report
        return self.generate_diagnostic_report()

def main():
    """Main function"""
    if len(sys.argv) < 2:
        print("Usage: python diagnose_mqtt_communication.py <broker_ip> [broker_port]")
        print("Example: python diagnose_mqtt_communication.py 192.168.1.100 1883")
        sys.exit(1)
    
    broker_ip = sys.argv[1]
    broker_port = int(sys.argv[2]) if len(sys.argv) > 2 else 1883
    
    diagnostic = MQTTDiagnostic(broker_ip, broker_port)
    report = diagnostic.run_full_diagnostic()
    
    print(report)
    
    # Save report to file
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"mqtt_diagnostic_report_{timestamp}.txt"
    
    try:
        with open(filename, 'w') as f:
            f.write(report)
        print(f"\n📄 Diagnostic report saved to: {filename}")
    except Exception as e:
        print(f"\n⚠️  Could not save report to file: {e}")

if __name__ == "__main__":
    main()
