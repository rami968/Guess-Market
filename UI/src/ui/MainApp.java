package ui;

import engine.service.EngineService;
import engine.service.EngineServiceImpl;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.controller.MainController;

import java.net.URL;

/**
 * JavaFX Application Launcher for Guess Market Exercise 2.
 */
public class MainApp extends Application {

    private EngineService engineService;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.engineService = new EngineServiceImpl();

        URL fxmlUrl = getClass().getResource("/ui/resources/main.fxml");
        if (fxmlUrl == null) {
            fxmlUrl = getClass().getResource("resources/main.fxml");
        }
        if (fxmlUrl == null) {
            throw new IllegalStateException("Could not find main.fxml resource file.");
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        MainController mainController = loader.getController();
        mainController.setPrimaryStage(primaryStage);
        mainController.setEngineService(engineService);

        Scene scene = new Scene(root, 1150, 750);

        URL cssUrl = getClass().getResource("/ui/resources/style.css");
        if (cssUrl == null) {
            cssUrl = getClass().getResource("resources/style.css");
        }
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setTitle("Guess Market - Prediction & Trading Platform (Exercise 2)");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
