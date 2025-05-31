// src/main/java/esp/proyecto/esptest/controllers/CryptoController.java
package esp.proyecto.esptest.controllers;

import esp.proyecto.esptest.AppContext;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;

public class CryptoController extends BaseController {
    @FXML private Label lblBTC, lblSOL;

    private final MqttClient mqttClient = AppContext.getInstance().getMqttClient();
    private final HttpClient  httpClient = HttpClient.newHttpClient();
    private ScheduledExecutorService scheduler;

    @FXML
    public void initialize() {
        // Arranca un scheduler que cada 5s consulta Binance y publica en MQTT
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::fetchAndPublish, 0, 5, TimeUnit.SECONDS);
    }

    private void fetchAndPublish() {
        try {
            // 1) Obtener precio BTC
            float priceBTC = fetchPrice("BTCUSDT");
            publish("crypto/BTCUSDT", priceBTC);
            // 2) Obtener precio SOL
            float priceSOL = fetchPrice("SOLUSDT");
            publish("crypto/SOLUSDT", priceSOL);
            // 3) Actualizar UI (JavaFX thread)
            Platform.runLater(() -> {
                lblBTC.setText(String.format("BTC: $%.2f", priceBTC));
                lblSOL.setText(String.format("SOL: $%.2f", priceSOL));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float fetchPrice(String symbol) throws Exception {
        String url = "https://data-api.binance.vision/api/v3/ticker/price?symbol=" + symbol;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        // payload: {"symbol":"BTCUSDT","price":"62000.12"}
        String body = resp.body();
        String priceStr = body.split("\"price\":\"")[1].split("\"")[0];
        return Float.parseFloat(priceStr);
    }

    private void publish(String topic, float price) {
        try {
            String payload = String.format("%.2f", price);
            MqttMessage msg = new MqttMessage(payload.getBytes());
            msg.setQos(1);
            mqttClient.publish(topic, msg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    protected void mainView() throws IOException {
        // Detenemos el scheduler para no seguir publicando
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        showScene("hello-view");
    }
}
