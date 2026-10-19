package ui.controller;

import engine.dto.ChatDTO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ui.http.HttpClientManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Controller for the bonus Chat Room tab.
 * Allows sending and receiving live broadcast messages with all connected traders.
 */
public class ChatTabController {

    @FXML private ListView<String> chatListView;
    @FXML private TextField messageInputField;
    @FXML private Button sendButton;
    @FXML private Label chatStatusLabel;

    private final ObservableList<String> chatItems = FXCollections.observableArrayList();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private String currentUsername;
    private MainController mainController;

    @FXML
    public void initialize() {
        if (chatListView != null) {
            chatListView.setItems(chatItems);
        }
    }

    public void setSession(String currentUsername, MainController mainController) {
        this.currentUsername = currentUsername;
        this.mainController = mainController;
    }

    @FXML
    private void handleSendMessage() {
        if (currentUsername == null || messageInputField == null) return;
        String text = messageInputField.getText();
        if (text == null || text.trim().isEmpty()) return;

        final String msg = text.trim();
        messageInputField.clear();

        // Send asynchronously to server
        new Thread(() -> {
            try {
                HttpClientManager.getInstance().sendChatMessage(currentUsername, msg);
                // Trigger polling sync immediately
                if (mainController != null) {
                    mainController.pollUpdates();
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (mainController != null) {
                        mainController.showAlert(Alert.AlertType.ERROR, "Chat Error", "Failed to send message: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    public void addMessages(List<ChatDTO> newMessages) {
        if (newMessages == null || newMessages.isEmpty()) return;

        Platform.runLater(() -> {
            for (ChatDTO msg : newMessages) {
                String timeStr = timeFormat.format(new Date(msg.getTimestamp()));
                String line = String.format("[%s]  %s:  %s", timeStr, msg.getUsername(), msg.getMessage());
                chatItems.add(line);
            }
            if (!chatItems.isEmpty() && chatListView != null) {
                chatListView.scrollTo(chatItems.size() - 1);
            }
        });
    }
}
