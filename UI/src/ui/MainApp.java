package ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.controller.LoginController;

import java.net.URL;

/**
 * JavaFX Application Launcher for Guess Market Exercise 3 (Client-Server).
 * Starts with the Login window for user registration/authentication.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        URL fxmlUrl = getClass().getResource("/ui/resources/login.fxml");
        if (fxmlUrl == null) {
            fxmlUrl = getClass().getResource("resources/login.fxml");
        }
        if (fxmlUrl == null) {
            throw new IllegalStateException("Could not find login.fxml resource file.");
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        LoginController loginController = loader.getController();
        loginController.setStage(primaryStage);

        Scene scene = new Scene(root, 480, 440);

        URL cssUrl = getClass().getResource("/ui/resources/style.css");
        if (cssUrl == null) {
            cssUrl = getClass().getResource("resources/style.css");
        }
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setTitle("Guess Market • Login");
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
