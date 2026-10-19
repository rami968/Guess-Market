package ui.controller;

import engine.dto.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ui.http.HttpClientManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the Events & Trading Tab.
 * Manages event filtering, drill-down details, dual Order Book tables, and trading actions.
 */
public class EventsTabController {

    // Filter Toggle Buttons
    @FXML private ToggleButton filterTypeAll;
    @FXML private ToggleButton filterTypeLMSR;
    @FXML private ToggleButton filterTypeOB;

    @FXML private ToggleButton filterStatusAll;
    @FXML private ToggleButton filterStatusActive;
    @FXML private ToggleButton filterStatusInactive;
    @FXML private ToggleButton filterStatusClosed;

    @FXML private ToggleButton filterFeeAll;
    @FXML private ToggleButton filterFeePurchase;
    @FXML private ToggleButton filterFeeClose;

    private ToggleGroup typeGroup;
    private ToggleGroup statusGroup;
    private ToggleGroup feeGroup;

    // Events Table
    @FXML private TableView<EventDTO> eventsTable;
    @FXML private TableColumn<EventDTO, Integer> colId;
    @FXML private TableColumn<EventDTO, String> colName;
    @FXML private TableColumn<EventDTO, String> colType;
    @FXML private TableColumn<EventDTO, String> colStatus;
    @FXML private TableColumn<EventDTO, String> colCommission;
    @FXML private TableColumn<EventDTO, String> colMarketMaker;
    @FXML private TableColumn<EventDTO, Double> colBalance;

    // Details View
    @FXML private Label eventNameLabel;
    @FXML private Label eventDescriptionLabel;
    @FXML private Label eventStatusLabel;
    @FXML private Label eventMmLabel;
    @FXML private Label eventBalanceLabel;
    @FXML private Button activateButton;
    @FXML private Button closeEventButton;

    // Order Book Dual Panels
    @FXML private VBox orderBookContainer;

    @FXML private Label opt1NameLabel;
    @FXML private Label opt1LastLabel;
    @FXML private Label opt1BidLabel;
    @FXML private Label opt1AskLabel;
    @FXML private Label opt1MidLabel;
    @FXML private Label opt1SpreadLabel;
    @FXML private TableView<OrderDTO> opt1BuyTable;
    @FXML private TableView<OrderDTO> opt1SellTable;

    @FXML private Label opt2NameLabel;
    @FXML private Label opt2LastLabel;
    @FXML private Label opt2BidLabel;
    @FXML private Label opt2AskLabel;
    @FXML private Label opt2MidLabel;
    @FXML private Label opt2SpreadLabel;
    @FXML private TableView<OrderDTO> opt2BuyTable;
    @FXML private TableView<OrderDTO> opt2SellTable;

    // Order Book Submission Form
    @FXML private ComboBox<String> userSelectorCombo;
    @FXML private ComboBox<String> optionSelectorCombo;
    @FXML private ComboBox<String> actionTypeCombo;
    @FXML private Spinner<Integer> countSpinner;
    @FXML private TextField priceField;
    @FXML private Button submitOrderButton;

    // LMSR Trading Form
    @FXML private VBox lmsrContainer;
    @FXML private ComboBox<String> lmsrUserCombo;
    @FXML private ComboBox<String> lmsrOptionCombo;
    @FXML private Spinner<Integer> lmsrCountSpinner;
    @FXML private Button lmsrBuyButton;

    private UserDTO currentUser;
    private MainController mainController;

    private final List<EventDTO> rawEventsList = new ArrayList<>();
    private EventDTO selectedEvent;

    public void setSession(UserDTO user, MainController mainController) {
        this.currentUser = user;
        this.mainController = mainController;

        if (userSelectorCombo != null) {
            userSelectorCombo.getItems().setAll(user.getName());
            userSelectorCombo.setValue(user.getName());
            userSelectorCombo.setDisable(true); // User trades for themselves
        }
        if (lmsrUserCombo != null) {
            lmsrUserCombo.getItems().setAll(user.getName());
            lmsrUserCombo.setValue(user.getName());
            lmsrUserCombo.setDisable(true);
        }
    }

    @FXML
    public void initialize() {
        setupFilterGroups();
        setupEventsTable();
        setupOrderBookTables();
        setupFormControls();
    }

    private void setupFilterGroups() {
        typeGroup = new ToggleGroup();
        filterTypeAll.setToggleGroup(typeGroup);
        filterTypeLMSR.setToggleGroup(typeGroup);
        filterTypeOB.setToggleGroup(typeGroup);
        typeGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) filterTypeAll.setSelected(true);
            applyFilters();
        });

        statusGroup = new ToggleGroup();
        filterStatusAll.setToggleGroup(statusGroup);
        filterStatusActive.setToggleGroup(statusGroup);
        filterStatusInactive.setToggleGroup(statusGroup);
        filterStatusClosed.setToggleGroup(statusGroup);
        statusGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) filterStatusAll.setSelected(true);
            applyFilters();
        });

        feeGroup = new ToggleGroup();
        filterFeeAll.setToggleGroup(feeGroup);
        filterFeePurchase.setToggleGroup(feeGroup);
        filterFeeClose.setToggleGroup(feeGroup);
        feeGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) filterFeeAll.setSelected(true);
            applyFilters();
        });
    }

    private void setupEventsTable() {
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEventType().name()));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        colCommission.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCommission() + "% (" + d.getValue().getCommissionType().name() + ")"));
        colMarketMaker.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMarketMakerName()));
        colBalance.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getAccountBalance()).asObject());

        eventsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                this.selectedEvent = newSel;
                displayEventDetails(newSel);
            }
        });
    }

    private void setupOrderBookTables() {
        setupOrderColumns(opt1BuyTable);
        setupOrderColumns(opt1SellTable);
        setupOrderColumns(opt2BuyTable);
        setupOrderColumns(opt2SellTable);
    }

    private void setupOrderColumns(TableView<OrderDTO> table) {
        if (table == null) return;
        table.getColumns().clear();

        TableColumn<OrderDTO, String> colUser = new TableColumn<>("User");
        colUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUserName()));
        colUser.setPrefWidth(90);

        TableColumn<OrderDTO, Integer> colShares = new TableColumn<>("Shares");
        colShares.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getSharesCount()).asObject());
        colShares.setPrefWidth(65);

        TableColumn<OrderDTO, Double> colPrice = new TableColumn<>("Price ($)");
        colPrice.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getPricePerShare()).asObject());
        colPrice.setPrefWidth(75);

        table.getColumns().addAll(colUser, colShares, colPrice);
    }

    private void setupFormControls() {
        if (actionTypeCombo != null) {
            actionTypeCombo.getItems().setAll("BUY", "SELL");
            actionTypeCombo.setValue("BUY");
        }
        if (countSpinner != null) {
            countSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 1));
        }
        if (lmsrCountSpinner != null) {
            lmsrCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 1));
        }

        if (activateButton != null) activateButton.setVisible(false);
        if (closeEventButton != null) closeEventButton.setVisible(false);
        if (orderBookContainer != null) orderBookContainer.setVisible(false);
        if (lmsrContainer != null) lmsrContainer.setVisible(false);
    }

    // ==================== Data Updates ====================

    public void updateEventsList(List<EventDTO> events) {
        Platform.runLater(() -> {
            rawEventsList.clear();
            if (events != null) {
                rawEventsList.addAll(events);
            }
            applyFilters();

            // Refresh selected event details if still present
            if (selectedEvent != null) {
                Optional<EventDTO> refreshed = rawEventsList.stream()
                        .filter(e -> e.getName().equalsIgnoreCase(selectedEvent.getName()))
                        .findFirst();
                if (refreshed.isPresent()) {
                    this.selectedEvent = refreshed.get();
                    displayEventDetails(selectedEvent);
                }
            }
        });
    }

    private void applyFilters() {
        if (eventsTable == null) return;

        List<EventDTO> filtered = rawEventsList.stream().filter(e -> {
            // Type filter
            if (filterTypeLMSR.isSelected() && e.getEventType() != EventType.LMSR) return false;
            if (filterTypeOB.isSelected() && e.getEventType() != EventType.ORDER_BOOK) return false;

            // Status filter
            if (filterStatusActive.isSelected() && e.getStatus() != EventStatus.ACTIVE) return false;
            if (filterStatusInactive.isSelected() && e.getStatus() != EventStatus.INACTIVE) return false;
            if (filterStatusClosed.isSelected() && e.getStatus() != EventStatus.CLOSED) return false;

            // Fee filter
            if (filterFeePurchase.isSelected() && e.getCommissionType() != CommissionType.ON_PURCHASE) return false;
            if (filterFeeClose.isSelected() && e.getCommissionType() != CommissionType.ON_CLOSE) return false;

            return true;
        }).collect(Collectors.toList());

        eventsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void displayEventDetails(EventDTO event) {
        if (event == null) return;

        eventNameLabel.setText(event.getName());
        eventDescriptionLabel.setText(event.getDescription());
        eventStatusLabel.setText("Status: " + event.getStatus().name());
        eventMmLabel.setText("Market Maker: " + event.getMarketMakerName());
        eventBalanceLabel.setText(String.format("Event Balance: $%.2f", event.getAccountBalance()));

        boolean isMM = currentUser != null && currentUser.getName().equalsIgnoreCase(event.getMarketMakerName());

        if (activateButton != null) {
            activateButton.setVisible(isMM && event.getStatus() == EventStatus.INACTIVE);
        }
        if (closeEventButton != null) {
            closeEventButton.setVisible(isMM && event.getStatus() == EventStatus.ACTIVE);
        }

        // Setup options dropdowns
        if (optionSelectorCombo != null) {
            optionSelectorCombo.getItems().setAll(event.getOption1(), event.getOption2());
            optionSelectorCombo.setValue(event.getOption1());
        }
        if (lmsrOptionCombo != null) {
            lmsrOptionCombo.getItems().setAll(event.getOption1(), event.getOption2());
            lmsrOptionCombo.setValue(event.getOption1());
        }

        if (event.getEventType() == EventType.ORDER_BOOK) {
            if (orderBookContainer != null) orderBookContainer.setVisible(true);
            if (lmsrContainer != null) lmsrContainer.setVisible(false);

            opt1NameLabel.setText("Option: " + event.getOption1());
            opt2NameLabel.setText("Option: " + event.getOption2());

            updateOrderBookStats(event);
        } else {
            if (orderBookContainer != null) orderBookContainer.setVisible(false);
            if (lmsrContainer != null) lmsrContainer.setVisible(true);
        }
    }

    private void updateOrderBookStats(EventDTO event) {
        OrderBookStatsDTO s1 = event.getOrderBookStatsOption1();
        OrderBookStatsDTO s2 = event.getOrderBookStatsOption2();

        if (s1 != null) {
            opt1LastLabel.setText(String.format("%.2f", s1.getLastPrice()));
            opt1BidLabel.setText(String.format("%.2f", s1.getHighestBid()));
            opt1AskLabel.setText(String.format("%.2f", s1.getLowestAsk()));
            opt1MidLabel.setText(String.format("%.2f", s1.getMidPrice()));
            opt1SpreadLabel.setText(String.format("%.2f", s1.getSpread()));
            opt1BuyTable.setItems(FXCollections.observableArrayList(s1.getBuyOrders()));
            opt1SellTable.setItems(FXCollections.observableArrayList(s1.getSellOrders()));
        }

        if (s2 != null) {
            opt2LastLabel.setText(String.format("%.2f", s2.getLastPrice()));
            opt2BidLabel.setText(String.format("%.2f", s2.getHighestBid()));
            opt2AskLabel.setText(String.format("%.2f", s2.getLowestAsk()));
            opt2MidLabel.setText(String.format("%.2f", s2.getMidPrice()));
            opt2SpreadLabel.setText(String.format("%.2f", s2.getSpread()));
            opt2BuyTable.setItems(FXCollections.observableArrayList(s2.getBuyOrders()));
            opt2SellTable.setItems(FXCollections.observableArrayList(s2.getSellOrders()));
        }
    }

    // ==================== Trading & Event Actions ====================

    @FXML
    private void handleActivateEvent() {
        if (selectedEvent == null || currentUser == null) return;

        Task<EventDTO> task = new Task<>() {
            @Override
            protected EventDTO call() throws Exception {
                return HttpClientManager.getInstance().activateEvent(selectedEvent.getName(), currentUser.getName());
            }
        };

        task.setOnSucceeded(e -> {
            mainController.showAlert(Alert.AlertType.INFORMATION, "Event Activated",
                    "Event '" + selectedEvent.getName() + "' is now ACTIVE for trading!");
            mainController.refreshAllData();
        });

        task.setOnFailed(e -> {
            mainController.showAlert(Alert.AlertType.ERROR, "Activation Failed", task.getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    private void handleCloseEvent() {
        if (selectedEvent == null || currentUser == null) return;

        List<String> options = List.of("1. " + selectedEvent.getOption1(), "2. " + selectedEvent.getOption2());
        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("Close Event & Declare Winner");
        dialog.setHeaderText("Select the winning outcome for event: " + selectedEvent.getName());
        dialog.setContentText("Winning Option:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            int winningIndex = result.get().startsWith("1") ? 1 : 2;

            Task<EventDTO> task = new Task<>() {
                @Override
                protected EventDTO call() throws Exception {
                    return HttpClientManager.getInstance().closeEvent(selectedEvent.getName(), currentUser.getName(), winningIndex);
                }
            };

            task.setOnSucceeded(e -> {
                mainController.showAlert(Alert.AlertType.INFORMATION, "Event Closed",
                        "Event resolved successfully! Payouts distributed and event account emptied.");
                mainController.refreshAllData();
            });

            task.setOnFailed(e -> {
                mainController.showAlert(Alert.AlertType.ERROR, "Closure Failed", task.getException().getMessage());
            });

            new Thread(task).start();
        }
    }

    @FXML
    private void handleSubmitOrder() {
        if (selectedEvent == null || currentUser == null) return;
        String option = optionSelectorCombo != null ? optionSelectorCombo.getValue() : null;
        String type = actionTypeCombo != null ? actionTypeCombo.getValue() : "BUY";
        int shares = countSpinner != null ? countSpinner.getValue() : 1;

        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
            if (price <= 0) {
                mainController.showAlert(Alert.AlertType.ERROR, "Invalid Input", "Price must be positive.");
                return;
            }
        } catch (Exception e) {
            mainController.showAlert(Alert.AlertType.ERROR, "Invalid Input", "Price per share must be a valid number.");
            return;
        }

        Task<EventDTO> task = new Task<>() {
            @Override
            protected EventDTO call() throws Exception {
                return HttpClientManager.getInstance().tradeOrderBook(selectedEvent.getName(), currentUser.getName(), option, type, shares, price);
            }
        };

        task.setOnSucceeded(e -> {
            mainController.showAlert(Alert.AlertType.INFORMATION, "Order Submitted", "Limit order submitted successfully!");
            mainController.refreshAllData();
        });

        task.setOnFailed(e -> {
            mainController.showAlert(Alert.AlertType.ERROR, "Order Error", task.getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    private void handleBuyLMSR() {
        if (selectedEvent == null || currentUser == null) return;
        String option = lmsrOptionCombo != null ? lmsrOptionCombo.getValue() : null;
        int shares = lmsrCountSpinner != null ? lmsrCountSpinner.getValue() : 1;
        int optionIndex = (option != null && option.equalsIgnoreCase(selectedEvent.getOption1())) ? 1 : 2;

        Task<PurchaseResultDTO> task = new Task<>() {
            @Override
            protected PurchaseResultDTO call() throws Exception {
                return HttpClientManager.getInstance().tradeLMSR(selectedEvent.getName(), currentUser.getName(), optionIndex, shares);
            }
        };

        task.setOnSucceeded(e -> {
            PurchaseResultDTO res = task.getValue();
            mainController.showAlert(Alert.AlertType.INFORMATION, "Shares Purchased",
                    String.format("Successfully bought %d shares of %s for $%.2f (Fee: $%.2f)", shares, option, res.getSharePrice(), res.getCommissionPaid()));
            mainController.refreshAllData();
        });

        task.setOnFailed(e -> {
            mainController.showAlert(Alert.AlertType.ERROR, "Purchase Error", task.getException().getMessage());
        });

        new Thread(task).start();
    }
}
