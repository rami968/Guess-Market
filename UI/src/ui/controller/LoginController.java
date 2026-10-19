package ui.controller;

import engine.dto.UserDTO;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ui.http.HttpClientManager;

import java.net.URL;

/**
 * Controller for the Login / Join Market window.
 * Authenticates the user with the Tomcat server before opening the main interface.
 */
public class LoginController {

    @FXML private TextField serverUrlField;
    @FXML private TextField usernameField;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button loginButton;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        if (statusLabel != null) {
            statusLabel.setText("");
        }
        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        if (username == null || username.trim().isEmpty()) {
            showError("Please enter a username.");
            return;
        }

        String serverUrl = serverUrlField.getText();
        if (serverUrl != null && !serverUrl.trim().isEmpty()) {
            HttpClientManager.getInstance().setBaseUrl(serverUrl.trim());
        }

        setLoading(true);
        statusLabel.setText("Connecting to server...");

        Task<UserDTO> loginTask = new Task<>() {
            @Override
            protected UserDTO call() throws Exception {
                HttpClientManager client = HttpClientManager.getInstance();
                // Check if server is reachable first
                if (!client.pingServer()) {
                    throw new RuntimeException("Cannot connect to server at " + client.getBaseUrl() + ". Is Tomcat running?");
                }
                return client.login(username.trim());
            }
        };

        loginTask.setOnSucceeded(e -> {
            setLoading(false);
            UserDTO loggedInUser = loginTask.getValue();
            openMainWindow(loggedInUser);
        });

        loginTask.setOnFailed(e -> {
            setLoading(false);
            Throwable err = loginTask.getException();
            String msg = (err != null && err.getMessage() != null) ? err.getMessage() : "Login failed.";
            showError(msg);
        });

        new Thread(loginTask).start();
    }

    private void openMainWindow(UserDTO user) {
        try {
            URL fxmlUrl = getClass().getResource("/ui/resources/main.fxml");
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getResource("resources/main.fxml");
            }
            if (fxmlUrl == null) {
                showError("Could not find main.fxml resource.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            MainController mainController = loader.getController();
            mainController.setPrimaryStage(stage);
            mainController.initializeUserSession(user);

            Scene scene = new Scene(root, 1150, 750);
            URL cssUrl = getClass().getResource("/ui/resources/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setScene(scene);
            stage.setTitle("Guess Market • " + user.getName() + " (Connected)");
            stage.setResizable(true);
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception ex) {
            showError("Failed to open main screen: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void showError(String message) {
        if (statusLabel != null) {
            statusLabel.setStyle("-fx-text-fill: #f5365c;");
            statusLabel.setText(message);
        }
    }

    private void setLoading(boolean loading) {
        if (loginButton != null) loginButton.setDisable(loading);
        if (usernameField != null) usernameField.setDisable(loading);
        if (serverUrlField != null) serverUrlField.setDisable(loading);
        if (progressIndicator != null) progressIndicator.setVisible(loading);
    }
}
