module esp.proyecto.esptest {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    // MQTT Paho
    requires org.eclipse.paho.client.mqttv3;

    // HTTP Client (Java 11+)
    requires java.net.http;
    requires com.google.gson;
    requires jakarta.mail;
    //requires sendgrid.java;
    //requires java.http.client;

    opens esp.proyecto.esptest to javafx.fxml;
    exports esp.proyecto.esptest;
    exports esp.proyecto.esptest.controllers;
    opens esp.proyecto.esptest.controllers to javafx.fxml;

}