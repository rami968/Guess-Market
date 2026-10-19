package ui.controller;

import engine.dto.UserDTO;
import engine.dto.UserSummaryDTO;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.*;

/**
 * Controller for the Users Tab in Exercise 3.
 * Left panel: lists all other connected system users and their balances.
 * Right panel: displays detailed personal account information, holdings, and transaction history.
 */
public class UsersTabController {

    // Other Users Table
    @FXML private TableView<UserSummaryDTO> usersTable;
    @FXML private TableColumn<UserSummaryDTO, String> colUserName;
    @FXML private TableColumn<UserSummaryDTO, Double> colUserBalance;
    @FXML private TableColumn<UserSummaryDTO, String> colUserRole;
    @FXML private Label usersCountLabel;

    // Current User Profile
    @FXML private Label selectedUserNameLabel;
    @FXML private Label userBalanceLabel;
    @FXML private Label userStatusLabel;
    @FXML private ListView<String> mmEventsListView;

    // Holdings Table
    @FXML private TableView<HoldingRow> participationsTable;
    @FXML private TableColumn<HoldingRow, String> colPartEventName;
    @FXML private TableColumn<HoldingRow, String> colPartHoldings;

    // Transaction History Table
    @FXML private TableView<UserDTO.UserTransactionDTO> transactionsTable;
    @FXML private TableColumn<UserDTO.UserTransactionDTO, String> colTxDesc;
    @FXML private TableColumn<UserDTO.UserTransactionDTO, Double> colTxAmount;
    @FXML private TableColumn<UserDTO.UserTransactionDTO, Double> colTxBalanceAfter;

    private UserDTO currentUser;
    private MainController mainController;

    public void setSession(UserDTO user, MainController mainController) {
        this.currentUser = user;
        this.mainController = mainController;
        updateUserDetails(user);
    }

    @FXML
    public void initialize() {
        setupOtherUsersTable();
        setupHoldingsTable();
        setupTransactionsTable();
    }

    private void setupOtherUsersTable() {
        if (usersTable == null) return;

        colUserName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colUserBalance.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getBalance()).asObject());
        colUserRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isMarketMaker() ? "Market Maker" : "Trader"));
    }

    private void setupHoldingsTable() {
        if (participationsTable == null) return;

        colPartEventName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEventName()));
        colPartHoldings.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getHoldingsSummary()));
    }

    private void setupTransactionsTable() {
        if (transactionsTable == null) return;

        colTxDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        colTxAmount.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getAmount()).asObject());
        colTxBalanceAfter.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getBalanceAfter()).asObject());
    }



    // ==================== Public Update Methods ====================

    public void updateOtherUsers(List<UserSummaryDTO> otherUsers) {
        Platform.runLater(() -> {
            if (usersTable != null && otherUsers != null) {
                usersTable.setItems(FXCollections.observableArrayList(otherUsers));
                if (usersCountLabel != null) {
                    usersCountLabel.setText("(" + otherUsers.size() + " users)");
                }
            }
        });
    }

    public void updateUserDetails(UserDTO user) {
        if (user == null) return;
        this.currentUser = user;

        Platform.runLater(() -> {
            if (selectedUserNameLabel != null) {
                selectedUserNameLabel.setText("👤 " + user.getName());
            }
            if (userStatusLabel != null) {
                userStatusLabel.setText(user.isMarketMaker() ? "Role: Market Maker" : "Role: Regular Trader");
                userStatusLabel.setStyle(user.isMarketMaker() ? "-fx-text-fill: #00f2fe; -fx-font-weight: bold;" : "-fx-text-fill: #8f92a1;");
            }
            if (userBalanceLabel != null) {
                userBalanceLabel.setText(String.format("$%.2f", user.getBalance()));
            }

            // MM owned events list
            if (mmEventsListView != null) {
                List<String> mmEvents = new ArrayList<>(user.getMarketMakerEventNames());
                if (mmEvents.isEmpty()) {
                    mmEventsListView.getItems().setAll("None (Upload XML to become Market Maker)");
                } else {
                    mmEventsListView.getItems().setAll(mmEvents);
                }
            }

            // Holdings
            if (participationsTable != null) {
                List<HoldingRow> rows = new ArrayList<>();
                Map<String, Map<String, Integer>> holdings = user.getHoldingsPerEventName();
                if (holdings != null) {
                    for (Map.Entry<String, Map<String, Integer>> entry : holdings.entrySet()) {
                        String eventName = entry.getKey();
                        StringBuilder summary = new StringBuilder();
                        for (Map.Entry<String, Integer> optEntry : entry.getValue().entrySet()) {
                            if (optEntry.getValue() > 0) {
                                if (summary.length() > 0) summary.append(", ");
                                summary.append(optEntry.getKey()).append(": ").append(optEntry.getValue()).append(" shares");
                            }
                        }
                        if (summary.length() > 0) {
                            rows.add(new HoldingRow(eventName, summary.toString()));
                        }
                    }
                }
                participationsTable.setItems(FXCollections.observableArrayList(rows));
            }

            // Transaction History
            if (transactionsTable != null) {
                List<UserDTO.UserTransactionDTO> txList = new ArrayList<>(user.getTransactions());
                Collections.reverse(txList); // Most recent first
                transactionsTable.setItems(FXCollections.observableArrayList(txList));
            }
        });
    }

    public static class HoldingRow {
        private final String eventName;
        private final String holdingsSummary;

        public HoldingRow(String eventName, String holdingsSummary) {
            this.eventName = eventName;
            this.holdingsSummary = holdingsSummary;
        }

        public String getEventName() { return eventName; }
        public String getHoldingsSummary() { return holdingsSummary; }
    }
}
