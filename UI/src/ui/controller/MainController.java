package ui.controller;

import engine.service.EngineService;
import engine.service.EngineServiceImpl;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ui.task.LoadXmlTask;

import java.io.File;

/**
 * Controller for the main application window.
 * Manages the top bar (File Load, Path display, Progress bar) and coordinates child controllers.
 */
public class MainController {

    @FXML private Button loadFileButton;
    @FXML private Label filePathLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;

    @FXML private TabPane mainTabPane;
    @FXML private Tab eventsTab;
    @FXML private Tab usersTab;

    @FXML private EventsTabController eventsTabViewController;
    @FXML private UsersTabController usersTabViewController;

    private EngineService engineService;
    private Stage primaryStage;

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setEngineService(EngineService engineService) {
        this.engineService = engineService;
        if (eventsTabViewController != null) {
            eventsTabViewController.setEngineService(engineService, this);
        }
        if (usersTabViewController != null) {
            usersTabViewController.setEngineService(engineService, this);
        }
    }

    @FXML
    public void initialize() {
        if (filePathLabel != null) {
            filePathLabel.setText("No file loaded");
        }
        if (progressBar != null) {
            progressBar.setVisible(false);
        }
        if (statusLabel != null) {
            statusLabel.setText("Ready");
        }
    }

    @FXML
    private void handleLoadFile() {
        if (primaryStage == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Guess Market XML Configuration File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files (*.xml)", "*.xml"));

        File initialDir = new File(System.getProperty("user.home"), "Downloads");
        if (initialDir.exists() && initialDir.isDirectory()) {
            fileChooser.setInitialDirectory(initialDir);
        }

        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            executeLoadTask(selectedFile);
        }
    }

    private void executeLoadTask(File file) {
        if (engineService == null) {
            engineService = new EngineServiceImpl();
        }

        LoadXmlTask task = new LoadXmlTask(engineService, file.getAbsolutePath());

        if (progressBar != null) {
            progressBar.setVisible(true);
            progressBar.progressProperty().bind(task.progressProperty());
        }
        if (statusLabel != null) {
            statusLabel.textProperty().bind(task.messageProperty());
        }
        if (loadFileButton != null) {
            loadFileButton.setDisable(true);
        }

        task.setOnSucceeded(event -> {
            if (progressBar != null) {
                progressBar.progressProperty().unbind();
                progressBar.setVisible(false);
            }
            if (statusLabel != null) {
                statusLabel.textProperty().unbind();
                statusLabel.setText("File loaded successfully!");
            }
            if (loadFileButton != null) {
                loadFileButton.setDisable(false);
            }
            if (filePathLabel != null) {
                filePathLabel.setText(file.getAbsolutePath());
            }

            refreshAllViews();
            showAlert(Alert.AlertType.INFORMATION, "Success", "XML Configuration file loaded successfully!");
        });

        task.setOnFailed(event -> {
            if (progressBar != null) {
                progressBar.progressProperty().unbind();
                progressBar.setVisible(false);
            }
            if (statusLabel != null) {
                statusLabel.textProperty().unbind();
                statusLabel.setText("Failed to load file.");
            }
            if (loadFileButton != null) {
                loadFileButton.setDisable(false);
            }

            Throwable ex = task.getException();
            String errorMsg = (ex != null) ? ex.getMessage() : "Unknown error during XML loading.";
            showAlert(Alert.AlertType.ERROR, "File Load Error", errorMsg);
        });

        Thread thread = new Thread(task, "xml-loader-thread");
        thread.setDaemon(true);
        thread.start();
    }

    public void refreshAllViews() {
        if (eventsTabViewController != null) {
            eventsTabViewController.refreshEvents();
        }
        if (usersTabViewController != null) {
            usersTabViewController.refreshUsers();
        }
    }

    public void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        if (primaryStage != null) {
            alert.initOwner(primaryStage);
        }
        alert.showAndWait();
    }
}
