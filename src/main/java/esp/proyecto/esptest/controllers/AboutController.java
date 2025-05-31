package esp.proyecto.esptest.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class AboutController {
    @FXML
    AnchorPane contenedor;

    private void showScene(String sceneName) throws IOException {
        AnchorPane pane = FXMLLoader.load(getClass().getResource("/esp/proyecto/esptest/" + sceneName + ".fxml"));
        contenedor.getChildren().setAll(pane);
    }

    @FXML
    protected void mainView() throws IOException {
        showScene("hello-view");
    }


}
