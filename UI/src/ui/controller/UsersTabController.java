package ui.controller;

import engine.dto.EventDTO;
import engine.dto.UserDTO;
import engine.service.EngineService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller for the Users Tab (Slide 2 of lecturer wireframe).
 * Displays user list, account balance, event participations/ownership, holdings, and transaction history.
 */
public class UsersTabController {

    // Users Table
    @FXML private TableView<UserDTO> usersTable;
    @FXML private TableColumn<UserDTO, String> colUserName;
    @FXML private TableColumn<UserDTO, Double> colUserBalance;
    @FXML private TableColumn<UserDTO, String> colUserStatus;

    // Single User Details Panel
    @FXML private Label selectedUserNameLabel;
    @FXML private Label userBalanceLabel;
    @FXML private Label userStatusLabel;
    @FXML private ListView<String> mmEventsListView;

    // Event Participations Table
    @FXML private TableView<ParticipationRow> participationsTable;
    @FXML private TableColumn<ParticipationRow, Integer> colPartEventId;
    @FXML private TableColumn<ParticipationRow, String> colPartEventName;
    @FXML private TableColumn<ParticipationRow, String> colPartHoldings;
    @FXML private TableColumn<ParticipationRow, String> colPartRole;

    // Transactions History Table
    @FXML private TableView<UserDTO.UserTransactionDTO> transactionsTable;
    @FXML private TableColumn<UserDTO.UserTransactionDTO, String> colTxDesc;
    @FXML private TableColumn<UserDTO.UserTransactionDTO, Double> colTxAmount;
    @FXML private TableColumn<UserDTO.UserTransactionDTO, Double> colTxBalanceAfter;

    private EngineService engineService;
    private MainController mainController;
    private UserDTO selectedUser;

    public void setEngineService(EngineService engineService, MainController mainController) {
        this.engineService = engineService;
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        setupUsersTable();
        setupParticipationsTable();
        setupTransactionsTable();
    }

    private void setupUsersTable() {
        if (usersTable == null) return;

        colUserName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colUserBalance.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getBalance()).asObject());
        colUserStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isBlocked() ? "BLOCKED" : "ACTIVE"));

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                displayUserDetails(newSel);
            }
        });
    }

    private void setupParticipationsTable() {
        if (participationsTable == null) return;

        colPartEventId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getEventId()).asObject());
        colPartEventName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEventName()));
        colPartHoldings.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getHoldingsSummary()));
        colPartRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole()));
    }

    private void setupTransactionsTable() {
        if (transactionsTable == null) return;

        colTxDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        colTxAmount.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getAmount()).asObject());
        colTxBalanceAfter.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getBalanceAfter()).asObject());
    }

    public void refreshUsers() {
        if (engineService == null || !engineService.isFileLoaded()) return;

        List<UserDTO> users = engineService.getAllUsers();
        usersTable.setItems(FXCollections.observableArrayList(users));

        if (selectedUser != null) {
            UserDTO updated = engineService.getUserDetails(selectedUser.getName());
            displayUserDetails(updated);
        } else if (!users.isEmpty()) {
            usersTable.getSelectionModel().select(0);
        }
    }

    private void displayUserDetails(UserDTO user) {
        if (user == null) return;
        this.selectedUser = user;

        if (selectedUserNameLabel != null) selectedUserNameLabel.setText("User: " + user.getName());
        if (userBalanceLabel != null) userBalanceLabel.setText(String.format("$%.2f", user.getBalance()));
        if (userStatusLabel != null) userStatusLabel.setText("Status: " + (user.isBlocked() ? "BLOCKED" : "ACTIVE"));

        // MM Events
        if (mmEventsListView != null) {
            List<String> mmList = new ArrayList<>();
            for (Integer eventId : user.getMarketMakerEventIds()) {
                try {
                    EventDTO ev = engineService.getEventDetails(eventId);
                    mmList.add("Event #" + eventId + ": " + ev.getName() + " (" + ev.getStatus().name() + ")");
                } catch (Exception ignored) {}
            }
            mmEventsListView.setItems(FXCollections.observableArrayList(mmList));
        }

        // Participations
        if (participationsTable != null && engineService != null) {
            List<ParticipationRow> rows = new ArrayList<>();
            for (EventDTO ev : engineService.getAllEvents()) {
                Map<String, Integer> holdings = user.getHoldingsPerEvent().getOrDefault(ev.getId(), null);
                boolean isMM = user.getMarketMakerEventIds().contains(ev.getId());

                if (isMM || (holdings != null && !holdings.isEmpty())) {
                    StringBuilder sb = new StringBuilder();
                    if (holdings != null) {
                        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
                            if (entry.getValue() > 0) {
                                if (sb.length() > 0) sb.append(", ");
                                sb.append(entry.getKey()).append(": ").append(entry.getValue());
                            }
                        }
                    }
                    if (sb.length() == 0) sb.append("No shares held");

                    String role = isMM ? "Market Maker" : "Trader";
                    rows.add(new ParticipationRow(ev.getId(), ev.getName(), sb.toString(), role));
                }
            }
            participationsTable.setItems(FXCollections.observableArrayList(rows));
        }

        // Transactions
        if (transactionsTable != null) {
            transactionsTable.setItems(FXCollections.observableArrayList(user.getTransactions()));
        }
    }

    public static class ParticipationRow {
        private final int eventId;
        private final String eventName;
        private final String holdingsSummary;
        private final String role;

        public ParticipationRow(int eventId, String eventName, String holdingsSummary, String role) {
            this.eventId = eventId;
            this.eventName = eventName;
            this.holdingsSummary = holdingsSummary;
            this.role = role;
        }

        public int getEventId() { return eventId; }
        public String getEventName() { return eventName; }
        public String getHoldingsSummary() { return holdingsSummary; }
        public String getRole() { return role; }
    }
}
