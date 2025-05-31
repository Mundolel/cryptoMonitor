package esp.proyecto.esptest;
import org.eclipse.paho.client.mqttv3.IMqttClient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
// SB
public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // please investigate what Parent is
        Parent root = FXMLLoader.load(getClass().getResource("hello-view.fxml"));
        Parent pin = FXMLLoader.load(getClass().getResource("pin-view.fxml"));
        Parent crypto = FXMLLoader.load(getClass().getResource("crypto-view.fxml"));
        Parent about = FXMLLoader.load(getClass().getResource("about-view.fxml"));
        Scene scene = new Scene(root, 500, 400);
        Scene scene2 = new Scene(pin, 500, 400);
        Scene scene3 = new Scene(crypto, 500, 400);
        Scene scene4 = new Scene(about, 500, 400);
        //scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm()); this is usual way
        String css = this.getClass().getResource("application.css").toExternalForm(); // investigate this
        scene.getStylesheets().add(css);
        scene2.getStylesheets().add(css);
        scene3.getStylesheets().add(css);
        scene4.getStylesheets().add(css);

        // if we have multiple scenes, we can just add the style by passing css as parameter
        stage.setTitle("The name of the application");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();

    }
}