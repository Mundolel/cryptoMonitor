package esp.proyecto.esptest.controllers;

import com.google.gson.Gson;
import esp.proyecto.esptest.AppContext;
import esp.proyecto.esptest.services.EmailService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class EmailController extends BaseController {
    private static final Logger LOGGER = Logger.getLogger(EmailController.class.getName());

    @FXML private TextField emailTxt;

    private MqttClient mqttClient;
    private final Gson gson = new Gson();
    private final EmailService emailService;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );

    // MQTT Topics
    private static final String TOPIC_EMAIL_NOTIFY = "notify/email";
    private static final String TOPIC_EMAIL_CONFIG = "config/email";

    public EmailController() {
        this.emailService = new EmailService();
    }

    @FXML
    public void initialize() {
        try {
            initializeMqttClient();
            subscribeToEmailNotifications();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during initialization", e);
            showErrorAlert("Error de inicialización", "No se pudo inicializar el controlador: " + e.getMessage());
        }
    }

    private void initializeMqttClient() throws MqttException {
        mqttClient = AppContext.getInstance().getMqttClient();

        if (!mqttClient.isConnected()) {
            throw new MqttException(Integer.parseInt("MQTT client is not connected"));
        }
    }

    private void subscribeToEmailNotifications() throws MqttException {
        mqttClient.subscribe(TOPIC_EMAIL_NOTIFY, (topic, msg) -> {
            try {
                processEmailNotification(msg);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error processing email notification", e);
            }
        });
    }

    private void processEmailNotification(MqttMessage msg) {
        String json = new String(msg.getPayload(), StandardCharsets.UTF_8);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> messageData = gson.fromJson(json, Map.class);

            String to = (String) messageData.get("to");
            String subject = (String) messageData.get("subject");
            String symbol = (String) messageData.get("symbol");
            Object priceObj = messageData.get("price");
            Object timestampObj = messageData.get("timestamp");

            if (to == null || symbol == null || priceObj == null) {
                LOGGER.warning("Incomplete email notification data: " + json);
                return;
            }

            if (!isValidEmail(to)) {
                LOGGER.warning("Invalid email address: " + to);
                return;
            }

            double price = ((Number) priceObj).doubleValue();
            long timestamp = timestampObj != null ? ((Number) timestampObj).longValue() : System.currentTimeMillis();

            String emailSubject = subject != null ? subject : "Crypto Price Alert: " + symbol;
            String emailBody = createEmailBody(symbol, price, timestamp);

            Platform.runLater(() -> sendEmail(to, emailSubject, emailBody));

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing email notification JSON: " + json, e);
        }
    }

    private String createEmailBody(String symbol, double price, long timestamp) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    .header { background-color: #f4f4f4; padding: 20px; border-radius: 5px; }
                    .content { margin: 20px 0; }
                    .price { font-size: 24px; font-weight: bold; color: #2c3e50; }
                    .symbol { color: #3498db; font-weight: bold; }
                    .timestamp { color: #7f8c8d; font-size: 12px; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h2>🚨 Crypto Price Alert</h2>
                </div>
                <div class="content">
                    <p>You have received a crypto price alert from your ESP8266 monitor:</p>
                    <p>Symbol: <span class="symbol">%s</span></p>
                    <p>Current Price: <span class="price">$%.2f</span></p>
                    <p class="timestamp">Alert generated at: %s</p>
                </div>
                <div class="footer">
                    <p><small>This alert was automatically generated by your ESP8266 Crypto Monitor.</small></p>
                </div>
            </body>
            </html>
            """, symbol, price, new java.util.Date(timestamp).toString());
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    @FXML
    protected void sendEmailConfig() {
        String email = emailTxt.getText().trim();

        if (!isValidEmail(email)) {
            showErrorAlert("Email inválido",
                    "Por favor, ingresa un email válido.\n" +
                            "Ejemplo: usuario@ejemplo.com");
            return;
        }

        publishEmailConfig(email)
                .thenRun(() -> Platform.runLater(() ->
                        showInfoAlert("Configuración enviada", "Email configurado: " + email)))
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            showErrorAlert("Error MQTT", "Error enviando configuración: " + ex.getMessage()));
                    return null;
                });
    }

    private CompletableFuture<Void> publishEmailConfig(String email) {
        return CompletableFuture.runAsync(() -> {
            try {
                String json = gson.toJson(Map.of("email", email));
                MqttMessage msg = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
                msg.setQos(1);
                mqttClient.publish(TOPIC_EMAIL_CONFIG, msg);

                LOGGER.info("Email configuration published: " + email);
            } catch (MqttException e) {
                throw new RuntimeException("Failed to publish email config", e);
            }
        });
    }

    private void sendEmail(String to, String subject, String body) {
        emailService.sendEmailAsync(to, subject, body)
                .thenRun(() -> {
                    LOGGER.info("Email sent successfully to: " + to);
                    Platform.runLater(() ->
                            showInfoAlert("Email enviado", "Notificación enviada exitosamente a: " + to));
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "Failed to send email", ex);
                    Platform.runLater(() ->
                            showErrorAlert("Error enviando email", "No se pudo enviar el email: " + ex.getMessage()));
                    return null;
                });
    }

    @FXML
    protected void testEmail() {
        String email = emailTxt.getText().trim();

        if (!isValidEmail(email)) {
            showErrorAlert("Email inválido", "Por favor, ingresa un email válido para la prueba.");
            return;
        }

        String testSubject = "Test - ESP8266 Crypto Monitor";
        String testBody = createEmailBody("BTC", 45000.00, System.currentTimeMillis());

        sendEmail(email, testSubject, testBody);
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    protected void mainView() throws IOException {
        cleanup();
        showScene("hello-view");
    }

    private void cleanup() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.unsubscribe(TOPIC_EMAIL_NOTIFY);
                LOGGER.info("Unsubscribed from email notifications");
            }
        } catch (MqttException e) {
            LOGGER.log(Level.WARNING, "Error during cleanup", e);
        }
    }
}