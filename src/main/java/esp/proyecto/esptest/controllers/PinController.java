package esp.proyecto.esptest.controllers;
// resolved dependencies
import esp.proyecto.esptest.AppContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;

public class PinController extends BaseController {
    @FXML AnchorPane contenedor;
    @FXML private ToggleButton tgGreen, tgWhite, tgRed;
    @FXML private Slider buzzerSlider;
    private MqttClient mqttClient;

    // MQTT part
    @FXML
    public void initialize() {
        // Obtenemos la instancia compartida
        mqttClient = AppContext.getInstance().getMqttClient();

        // Ahora usamos mqttClient sin recrearlo
        tgGreen.setOnAction(e -> publish("led/green", tgGreen.isSelected() ? "ON" : "OFF"));
        tgWhite.setOnAction(e -> publish("led/white", tgWhite.isSelected() ? "ON" : "OFF"));
        tgRed.setOnAction(e -> publish("led/red",   tgRed.isSelected()   ? "ON" : "OFF"));

        // cambios petristas:
        // Asumimos rango slider 0–1023, but we gonna implement just 1000 max
        buzzerSlider.setOnMouseReleased(e -> {
            int intensity = (int) buzzerSlider.getValue();
            publish("buzzer/intensity", String.valueOf(intensity));
        });
        // this buzzer new function ensures that we don't overload the messagin between our user and the broker
        // as we move the slider, by typical way, every slight change will send an MQTT publish to the broker
    }

    private void publish(String topic, String payload) {
        try {
            MqttMessage msg = new MqttMessage(payload.getBytes());
            msg.setQos(1);
            mqttClient.publish(topic, msg);
        } catch (Exception ex) {
            ex.printStackTrace();
            // Aquí podrías mostrar una alerta de error en la UI
        }
    }

    @FXML
    protected void mainView() throws IOException { // inherited from BaseController so that code it's not duplicated
        showScene("hello-view"); // the .fxml is done in the showScene function
    }

}
