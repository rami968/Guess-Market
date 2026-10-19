package ui.controller;

import engine.dto.ChatDTO;
import engine.dto.EventDTO;
import engine.dto.UserDTO;
import engine.dto.UserSummaryDTO;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ui.http.HttpClientManager;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Top-level controller managing the client application window, top bar,
 * background polling service, and coordination between child controllers.
 */
public class MainController {

    @FXML private Label userGreetingLabel;
    @FXML private Label balanceHeaderLabel;
    @FXML private Button chargeButton;
    @FXML private Button uploadXmlButton;
    @FXML private Label statusLabel;
    @FXML private Label syncVersionLabel;
    @FXML private ProgressBar progressBar;

    @FXML private TabPane mainTabPane;
    @FXML private Tab eventsTab;
    @FXML private Tab usersTab;
    @FXML private Tab chatTab;

    @FXML private EventsTabController eventsTabViewController;
    @FXML private UsersTabController usersTabViewController;
    @FXML private ChatTabController chatTabViewController;

    private Stage primaryStage;
    private UserDTO currentUser;
    private int currentSyncVersion = -1;
    private int currentChatIndex = 0;

    private ScheduledExecutorService pollingExecutor;

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setOnCloseRequest(e -> stopPolling());
    }

    public void initializeUserSession(UserDTO user) {
        this.currentUser = user;
        updateUserHeader();

        if (eventsTabViewController != null) {
            eventsTabViewController.setSession(currentUser, this);
        }
        if (usersTabViewController != null) {
            usersTabViewController.setSession(currentUser, this);
        }
        if (chatTabViewController != null) {
            chatTabViewController.setSession(currentUser.getName(), this);
        }

        // Initial fetch of data
        refreshAllData();

        // Start background polling
        startPolling();
    }

    public UserDTO getCurrentUser() {
        return currentUser;
    }

    public void updateUserHeader() {
        if (currentUser == null) return;
        Platform.runLater(() -> {
            if (userGreetingLabel != null) {
                String roleBadge = currentUser.isMarketMaker() ? " [Market Maker]" : "";
                userGreetingLabel.setText("👤 " + currentUser.getName() + roleBadge);
            }
            if (balanceHeaderLabel != null) {
                balanceHeaderLabel.setText(String.format("Balance: $%.2f", currentUser.getBalance()));
            }
        });
    }

    // ==================== Top Bar Actions ====================

    @FXML
    public void handleChargeFunds() {
        if (currentUser == null) return;

        TextInputDialog dialog = new TextInputDialog("100.00");
        dialog.setTitle("Deposit Funds");
        dialog.setHeaderText("Deposit funds into account: " + currentUser.getName());
        dialog.setContentText("Enter amount ($):");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            double amount;
            try {
                amount = Double.parseDouble(result.get().trim());
                if (amount <= 0) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Amount", "Deposit amount must be positive.");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid numeric amount.");
                return;
            }

            Task<UserDTO> chargeTask = new Task<>() {
                @Override
                protected UserDTO call() throws Exception {
                    return HttpClientManager.getInstance().chargeBalance(currentUser.getName(), amount);
                }
            };

            chargeTask.setOnSucceeded(e -> {
                this.currentUser = chargeTask.getValue();
                updateUserHeader();
                if (usersTabViewController != null) {
                    usersTabViewController.updateUserDetails(currentUser);
                }
                showAlert(Alert.AlertType.INFORMATION, "Deposit Successful",
                        String.format("Successfully deposited $%.2f. New balance: $%.2f", amount, currentUser.getBalance()));
            });

            chargeTask.setOnFailed(e -> {
                showAlert(Alert.AlertType.ERROR, "Deposit Error", chargeTask.getException().getMessage());
            });

            new Thread(chargeTask).start();
        }
    }

    @FXML
    public void handleUploadXml() {
        if (primaryStage == null || currentUser == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Guess Market XML Configuration");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files (*.xml)", "*.xml"));

        File initialDir = new File(System.getProperty("user.home"), "Downloads");
        if (initialDir.exists() && initialDir.isDirectory()) {
            fileChooser.setInitialDirectory(initialDir);
        }

        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            executeUploadTask(selectedFile);
        }
    }

    private void executeUploadTask(File file) {
        if (progressBar != null) progressBar.setVisible(true);
        if (uploadXmlButton != null) uploadXmlButton.setDisable(true);
        if (statusLabel != null) statusLabel.setText("Uploading XML file: " + file.getName() + "...");

        Task<String> uploadTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return HttpClientManager.getInstance().uploadXml(file, currentUser.getName());
            }
        };

        uploadTask.setOnSucceeded(e -> {
            if (progressBar != null) progressBar.setVisible(false);
            if (uploadXmlButton != null) uploadXmlButton.setDisable(false);
            if (statusLabel != null) statusLabel.setText("🟢 XML loaded successfully");

            showAlert(Alert.AlertType.INFORMATION, "Upload Succeeded", uploadTask.getValue());
            refreshAllData();
        });

        uploadTask.setOnFailed(e -> {
            if (progressBar != null) progressBar.setVisible(false);
            if (uploadXmlButton != null) uploadXmlButton.setDisable(false);
            if (statusLabel != null) statusLabel.setText("🔴 Upload failed");

            showAlert(Alert.AlertType.ERROR, "Upload Failed", uploadTask.getException().getMessage());
        });

        new Thread(uploadTask).start();
    }

    // ==================== Data Refresh & Polling ====================

    public void refreshAllData() {
        if (currentUser == null) return;

        Task<Void> refreshTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                HttpClientManager client = HttpClientManager.getInstance();

                // 1. Fetch updated events
                List<EventDTO> events = client.getAllEvents();

                // 2. Fetch updated current user details
                UserDTO updatedUser = client.getUserDetails(currentUser.getName());

                // 3. Fetch user summaries
                List<UserSummaryDTO> otherUsers = client.getAllUserSummaries();

                Platform.runLater(() -> {
                    currentUser = updatedUser;
                    updateUserHeader();

                    if (eventsTabViewController != null) {
                        eventsTabViewController.updateEventsList(events);
                    }
                    if (usersTabViewController != null) {
                        usersTabViewController.updateUserDetails(currentUser);
                        usersTabViewController.updateOtherUsers(otherUsers);
                    }
                });
                return null;
            }
        };

        new Thread(refreshTask).start();
    }

    private void startPolling() {
        if (pollingExecutor != null && !pollingExecutor.isShutdown()) {
            return;
        }

        pollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GuessMarket-Polling-Thread");
            t.setDaemon(true);
            return t;
        });

        // Run polling every 1 second
        pollingExecutor.scheduleWithFixedDelay(this::pollUpdates, 1, 1, TimeUnit.SECONDS);
    }

    public void pollUpdates() {
        try {
            HttpClientManager client = HttpClientManager.getInstance();

            // 1. Check state sync version
            HttpClientManager.SyncResult sync = client.checkSync();
            if (sync != null) {
                if (sync.version != currentSyncVersion) {
                    currentSyncVersion = sync.version;
                    Platform.runLater(() -> {
                        if (syncVersionLabel != null) {
                            syncVersionLabel.setText("Sync Version: v" + currentSyncVersion);
                        }
                    });
                    refreshAllData();
                }
            }

            // 2. Fetch chat updates
            HttpClientManager.ChatResponse chatResp = client.getChatMessages(currentChatIndex);
            if (chatResp != null && chatResp.messages != null && !chatResp.messages.isEmpty()) {
                currentChatIndex = chatResp.nextIndex;
                if (chatTabViewController != null) {
                    chatTabViewController.addMessages(chatResp.messages);
                }
            }

        } catch (Exception ignored) {
            // Server temporarily unreachable or busy, continue next cycle
        }
    }

    public void stopPolling() {
        if (pollingExecutor != null && !pollingExecutor.isShutdown()) {
            pollingExecutor.shutdownNow();
        }
    }

    public void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
