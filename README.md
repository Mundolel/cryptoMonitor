# cryptoMonitor
Crypto monitor proyect developed with edge computing of a ESP8266 NodeMCU v1.0

This JavaFX application allows users to interact with an ESP8266-based cryptocurrency monitor over MQTT. The app provides a GUI to:

* Configure which cryptocurrencies the ESP8266 will track.

* Set thresholds for triggering buzzer alerts on the ESP.

* Send and update an email address for alert notifications.

* Receive and forward real-time alerts from the ESP to the configured email using an SMTP backend.

* Test email delivery directly from the app for quick diagnostics.

The ESP8266 handles MQTT subscriptions, displays live prices on an OLED screen, and sends data when hardware buttons are pressed. This desktop client bridges configuration and notification tasks between the user and the microcontroller.

For further information, explication and functioning, please see: (https://youtu.be/TZTlfG7Ivfg)
