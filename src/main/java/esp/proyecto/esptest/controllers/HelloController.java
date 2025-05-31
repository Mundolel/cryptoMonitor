package esp.proyecto.esptest.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class HelloController extends BaseController {
    @FXML
    AnchorPane contenedor;
    /*

    public void showScene(String sceneName) throws IOException {
        AnchorPane pane = FXMLLoader.load(getClass().getResource("/esp/proyecto/esptest/" + sceneName + ".fxml"));
        contenedor.getChildren().setAll(pane);
    }
    Other way to switch stages: https://www.youtube.com/watch?v=hcM-R-YOKkQ bro code
    for this you gotta have a SceneController
    */

    @FXML
    protected void cryptoView() throws IOException {
        showScene("crypto-view"); // the .fxml is done in the showScene function
    }

    @FXML
    protected void pinView() throws IOException {
        showScene("pin-view"); // the .fxml is done in the showScene function
    }

    @FXML
    protected void aboutView() throws IOException {
        showScene("about-view");
    }

    @FXML
    protected void configView() throws IOException {
        showScene("config-view");
    }

    @FXML
    protected void emailView() throws IOException {
        showScene("email-view");
    }

}