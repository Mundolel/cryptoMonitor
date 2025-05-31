package esp.proyecto.esptest;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

public class AppContext {
    // Instancia única
    private static AppContext instance;

    // Cliente MQTT compartido
    private MqttClient mqttClient;

    private AppContext() {
        // Constructor privado para evitar instancias externas
        try {
            String brokerUrl = "tcp://broker.hivemq.com:1883";
            String clientId  = MqttClient.generateClientId();
            mqttClient = new MqttClient(brokerUrl, clientId);

            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setCleanSession(true);
            opts.setAutomaticReconnect(true);

            mqttClient.connect(opts);
        } catch (Exception e) {
            throw new RuntimeException("Error al inicializar MQTT", e);
        }
    }

    /**
     * Devuelve la instancia única de AppContext (lazy initialization).
     */
    public static synchronized AppContext getInstance() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }

    /**
     * Acceso al cliente MQTT compartido.
     */
    public MqttClient getMqttClient() {
        return mqttClient;
    }

    // Aquí podrías añadir otros recursos compartidos: configuraciones, caches, etc.
}
