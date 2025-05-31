package esp.proyecto.esptest.controllers;

import com.google.gson.Gson;
import esp.proyecto.esptest.AppContext;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigController extends BaseController {
    @FXML private ComboBox<String> cbButton1, cbButton2;
    @FXML private TextField tfThreshold1, tfThreshold2;
    @FXML private Spinner<Integer> spDuration;
    @FXML private Button btnSend;

    private MqttClient mqttClient;
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        // 1) Obtener cliente MQTT compartido
        mqttClient = AppContext.getInstance().getMqttClient();

        // 2) Llenar combos con monedas disponibles
        cbButton1.setItems(FXCollections.observableArrayList("BTCUSDT", "SOLUSDT", "ETHUSDT"));
        cbButton2.setItems(FXCollections.observableArrayList("BTCUSDT", "SOLUSDT", "ETHUSDT"));
        cbButton1.getSelectionModel().select(0);
        cbButton2.getSelectionModel().select(1);

        // 3) Configurar spinner de duración 100–10000 ms
        SpinnerValueFactory<Integer> factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 10000, 1000, 100);
        spDuration.setValueFactory(factory);

        // 4) Botón de envío
        btnSend.setOnAction(e -> sendConfiguration());
        // this can be set both in action of button in view or directly in the controller, in this case is the controller
        // if you want to make the action related to the button remember to specify FXML to the function
    }

    // @FXML
    protected void sendConfiguration() {
        try {
            // 1) Leer valores del formulario
            String m1 = cbButton1.getValue();
            String m2 = cbButton2.getValue();
            float t1 = Float.parseFloat(tfThreshold1.getText());
            float t2 = Float.parseFloat(tfThreshold2.getText());
            int duration = spDuration.getValue();

            // 2) Construir objeto de configuración
            Map<String, Object> config = new HashMap<>();
            config.put("button1", m1);
            config.put("button2", m2);

            Map<String, Float> thresholds = new HashMap<>();
            thresholds.put(m1, t1);
            thresholds.put(m2, t2);
            config.put("thresholds", thresholds);

            config.put("alarmDurationMs", duration);

            // 3) Serializar a JSON
            String json = gson.toJson(config);

            // 4) Publicar en MQTT en el tópico de configuración
            MqttMessage msg = new MqttMessage(json.getBytes());
            msg.setQos(1);
            mqttClient.publish("config/monitor01", msg);

            // 5) Feedback al usuario
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Configuración enviada.", ButtonType.OK);
            alert.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    protected void mainView() throws IOException {
        showScene("hello-view");
    }
}
