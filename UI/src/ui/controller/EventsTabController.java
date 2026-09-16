package ui.controller;

import engine.dto.*;
import engine.model.EventStatus;
import engine.model.EventType;
import engine.service.EngineService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

/**
 * Controller for the Events Tab (Slide 1 of lecturer wireframe).
 * Handles filtering, event summary table, drill-down event details, dual Order Book views, and trading.
 */
public class EventsTabController {

    // Filters
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> commissionFilterCombo;

    // Events Table
    @FXML private TableView<EventDTO> eventsTable;
    @FXML private TableColumn<EventDTO, Integer> colId;
    @FXML private TableColumn<EventDTO, String> colName;
    @FXML private TableColumn<EventDTO, String> colType;
    @FXML private TableColumn<EventDTO, String> colStatus;
    @FXML private TableColumn<EventDTO, String> colCommission;
    @FXML private TableColumn<EventDTO, String> colMarketMaker;
    @FXML private TableColumn<EventDTO, Double> colBalance;

    // Details View Controls
    @FXML private Label eventNameLabel;
    @FXML private Label eventDescriptionLabel;
    @FXML private Label eventStatusLabel;
    @FXML private Label eventMmLabel;
    @FXML private Label eventBalanceLabel;
    @FXML private Button activateButton;
    @FXML private Button closeEventButton;

    // Order Book Panels (Option 1 & Option 2)
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

    // Order Submission Form
    @FXML private ComboBox<String> userSelectorCombo;
    @FXML private ComboBox<String> optionSelectorCombo;
    @FXML private ComboBox<String> actionTypeCombo;
    @FXML private Spinner<Integer> countSpinner;
    @FXML private TextField priceField;
    @FXML private Button submitOrderButton;

    // LMSR Trading Form
    @FXML private VBox lmsrContainer;
    @FXML private ComboBox<String> lmsrOptionCombo;
    @FXML private Spinner<Integer> lmsrCountSpinner;
    @FXML private Button lmsrBuyButton;

    private EngineService engineService;
    private MainController mainController;
    private EventDTO selectedEvent;

    public void setEngineService(EngineService engineService, MainController mainController) {
        this.engineService = engineService;
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        setupFilters();
        setupEventsTable();
        setupOrderBookTables();
        setupFormControls();
        
        if (orderBookContainer != null) orderBookContainer.setVisible(false);
        if (lmsrContainer != null) lmsrContainer.setVisible(false);
    }

    private void setupFilters() {
        if (typeFilterCombo != null) {
            typeFilterCombo.setItems(FXCollections.observableArrayList("ALL", "LMSR", "ORDER_BOOK"));
            typeFilterCombo.setValue("ALL");
            typeFilterCombo.setOnAction(e -> refreshEvents());
        }
        if (statusFilterCombo != null) {
            statusFilterCombo.setItems(FXCollections.observableArrayList("ALL", "INACTIVE", "ACTIVE", "CLOSED"));
            statusFilterCombo.setValue("ALL");
            statusFilterCombo.setOnAction(e -> refreshEvents());
        }
        if (commissionFilterCombo != null) {
            commissionFilterCombo.setItems(FXCollections.observableArrayList("ALL", "ON_PURCHASE", "ON_CLOSE"));
            commissionFilterCombo.setValue("ALL");
            commissionFilterCombo.setOnAction(e -> refreshEvents());
        }
    }

    private void setupEventsTable() {
        if (eventsTable == null) return;

        colId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEventType().name()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        colCommission.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCommission() + "% (" + data.getValue().getCommissionType().name() + ")"));
        colMarketMaker.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMarketMakerName()));
        colBalance.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getAccountBalance()).asObject());

        eventsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                this.selectedEvent = newSel;
                displayEventDetails(newSel);
            }
        });
    }

    private void setupOrderBookTables() {
        setupOrderTableColumns(opt1BuyTable);
        setupOrderTableColumns(opt1SellTable);
        setupOrderTableColumns(opt2BuyTable);
        setupOrderTableColumns(opt2SellTable);
    }

    private void setupOrderTableColumns(TableView<OrderDTO> table) {
        if (table == null) return;
        table.getColumns().clear();

        TableColumn<OrderDTO, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUserName()));

        TableColumn<OrderDTO, Integer> sharesCol = new TableColumn<>("Shares");
        sharesCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getSharesCount()).asObject());

        TableColumn<OrderDTO, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getPricePerShare()).asObject());

        table.getColumns().addAll(userCol, sharesCol, priceCol);
    }

    private void setupFormControls() {
        if (actionTypeCombo != null) {
            actionTypeCombo.setItems(FXCollections.observableArrayList("BUY", "SELL"));
            actionTypeCombo.setValue("BUY");
        }
        if (countSpinner != null) {
            countSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 10));
        }
        if (lmsrCountSpinner != null) {
            lmsrCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 10));
        }
    }

    public void refreshEvents() {
        if (engineService == null || !engineService.isFileLoaded()) return;

        String typeF = typeFilterCombo != null ? typeFilterCombo.getValue() : "ALL";
        String statusF = statusFilterCombo != null ? statusFilterCombo.getValue() : "ALL";
        String commF = commissionFilterCombo != null ? commissionFilterCombo.getValue() : "ALL";

        List<EventDTO> filtered = engineService.getFilteredEvents(typeF, statusF, commF);
        eventsTable.setItems(FXCollections.observableArrayList(filtered));

        // Refresh user selectors
        List<UserDTO> users = engineService.getAllUsers();
        ObservableList<String> usernames = FXCollections.observableArrayList();
        for (UserDTO u : users) {
            usernames.add(u.getName());
        }
        if (userSelectorCombo != null) {
            userSelectorCombo.setItems(usernames);
            if (!usernames.isEmpty() && userSelectorCombo.getValue() == null) {
                userSelectorCombo.setValue(usernames.get(0));
            }
        }

        if (selectedEvent != null) {
            EventDTO updated = engineService.getEventDetails(selectedEvent.getId());
            displayEventDetails(updated);
        }
    }

    private void displayEventDetails(EventDTO event) {
        if (event == null) return;
        this.selectedEvent = event;

        if (eventNameLabel != null) eventNameLabel.setText(event.getName());
        if (eventDescriptionLabel != null) eventDescriptionLabel.setText(event.getDescription());
        if (eventStatusLabel != null) eventStatusLabel.setText("Status: " + event.getStatus().name());
        if (eventMmLabel != null) eventMmLabel.setText("Market Maker: " + event.getMarketMakerName());
        if (eventBalanceLabel != null) eventBalanceLabel.setText(String.format("Event Balance: $%.2f", event.getAccountBalance()));

        // Activation / Closure buttons
        if (activateButton != null) {
            activateButton.setDisable(event.getStatus() != EventStatus.INACTIVE);
        }
        if (closeEventButton != null) {
            closeEventButton.setDisable(event.getStatus() != EventStatus.ACTIVE);
        }

        // Toggle UI panels based on EventType
        if (event.getEventType() == EventType.ORDER_BOOK) {
            if (orderBookContainer != null) orderBookContainer.setVisible(true);
            if (lmsrContainer != null) lmsrContainer.setVisible(false);

            updateOrderBookStats(event);
        } else {
            if (orderBookContainer != null) orderBookContainer.setVisible(false);
            if (lmsrContainer != null) lmsrContainer.setVisible(true);

            if (lmsrOptionCombo != null) {
                lmsrOptionCombo.setItems(FXCollections.observableArrayList(event.getOption1(), event.getOption2()));
                lmsrOptionCombo.setValue(event.getOption1());
            }
        }

        if (optionSelectorCombo != null) {
            optionSelectorCombo.setItems(FXCollections.observableArrayList(event.getOption1(), event.getOption2()));
            optionSelectorCombo.setValue(event.getOption1());
        }
    }

    private void updateOrderBookStats(EventDTO event) {
        OrderBookStatsDTO s1 = event.getOrderBookStatsOption1();
        if (s1 != null) {
            if (opt1NameLabel != null) opt1NameLabel.setText(s1.getOptionName());
            if (opt1LastLabel != null) opt1LastLabel.setText(String.format("$%.2f", s1.getLastPrice()));
            if (opt1BidLabel != null) opt1BidLabel.setText(String.format("$%.2f", s1.getHighestBid()));
            if (opt1AskLabel != null) opt1AskLabel.setText(String.format("$%.2f", s1.getLowestAsk()));
            if (opt1MidLabel != null) opt1MidLabel.setText(String.format("$%.2f", s1.getMidPrice()));
            if (opt1SpreadLabel != null) opt1SpreadLabel.setText(String.format("$%.2f", s1.getSpread()));

            if (opt1BuyTable != null) opt1BuyTable.setItems(FXCollections.observableArrayList(s1.getBuyOrders()));
            if (opt1SellTable != null) opt1SellTable.setItems(FXCollections.observableArrayList(s1.getSellOrders()));
        }

        OrderBookStatsDTO s2 = event.getOrderBookStatsOption2();
        if (s2 != null) {
            if (opt2NameLabel != null) opt2NameLabel.setText(s2.getOptionName());
            if (opt2LastLabel != null) opt2LastLabel.setText(String.format("$%.2f", s2.getLastPrice()));
            if (opt2BidLabel != null) opt2BidLabel.setText(String.format("$%.2f", s2.getHighestBid()));
            if (opt2AskLabel != null) opt2AskLabel.setText(String.format("$%.2f", s2.getLowestAsk()));
            if (opt2MidLabel != null) opt2MidLabel.setText(String.format("$%.2f", s2.getMidPrice()));
            if (opt2SpreadLabel != null) opt2SpreadLabel.setText(String.format("$%.2f", s2.getSpread()));

            if (opt2BuyTable != null) opt2BuyTable.setItems(FXCollections.observableArrayList(s2.getBuyOrders()));
            if (opt2SellTable != null) opt2SellTable.setItems(FXCollections.observableArrayList(s2.getSellOrders()));
        }
    }

    @FXML
    private void handleActivateEvent() {
        if (selectedEvent == null || engineService == null) return;
        try {
            engineService.activateEvent(selectedEvent.getId(), selectedEvent.getMarketMakerName());
            mainController.showAlert(Alert.AlertType.INFORMATION, "Event Activated", "Event '" + selectedEvent.getName() + "' activated successfully by MM " + selectedEvent.getMarketMakerName() + "!");
            mainController.refreshAllViews();
        } catch (Exception e) {
            mainController.showAlert(Alert.AlertType.ERROR, "Activation Error", e.getMessage());
        }
    }

    @FXML
    private void handleCloseEvent() {
        if (selectedEvent == null || engineService == null) return;

        List<String> options = Arrays.asList("1. " + selectedEvent.getOption1(), "2. " + selectedEvent.getOption2());
        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("Close Event & Select Winner");
        dialog.setHeaderText("Select the winning outcome for event: " + selectedEvent.getName());
        dialog.setContentText("Winning Option:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            int winningIndex = result.get().startsWith("1") ? 1 : 2;
            try {
                engineService.closeEvent(selectedEvent.getId(), selectedEvent.getMarketMakerName(), winningIndex);
                mainController.showAlert(Alert.AlertType.INFORMATION, "Event Closed", "Event resolved successfully!");
                mainController.refreshAllViews();
            } catch (Exception e) {
                mainController.showAlert(Alert.AlertType.ERROR, "Closure Error", e.getMessage());
            }
        }
    }

    @FXML
    private void handleSubmitOrder() {
        if (selectedEvent == null || engineService == null) return;
        String user = userSelectorCombo != null ? userSelectorCombo.getValue() : null;
        String option = optionSelectorCombo != null ? optionSelectorCombo.getValue() : null;
        String type = actionTypeCombo != null ? actionTypeCombo.getValue() : "BUY";
        int shares = countSpinner != null ? countSpinner.getValue() : 1;

        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
        } catch (Exception e) {
            mainController.showAlert(Alert.AlertType.ERROR, "Invalid Input", "Price per share must be a valid number.");
            return;
        }

        try {
            engineService.submitOrder(selectedEvent.getId(), user, option, type, shares, price);
            mainController.showAlert(Alert.AlertType.INFORMATION, "Order Submitted", "Order submitted successfully!");
            mainController.refreshAllViews();
        } catch (Exception e) {
            mainController.showAlert(Alert.AlertType.ERROR, "Order Error", e.getMessage());
        }
    }

    @FXML
    private void handleBuyLMSR() {
        if (selectedEvent == null || engineService == null) return;
        String user = userSelectorCombo != null ? userSelectorCombo.getValue() : null;
        String option = lmsrOptionCombo != null ? lmsrOptionCombo.getValue() : null;
        int shares = lmsrCountSpinner != null ? lmsrCountSpinner.getValue() : 1;
        int optionIndex = option.equalsIgnoreCase(selectedEvent.getOption1()) ? 1 : 2;

        try {
            PurchaseResultDTO res = engineService.buySharesLMSR(selectedEvent.getId(), user, optionIndex, shares);
            mainController.showAlert(Alert.AlertType.INFORMATION, "Shares Purchased", String.format("Successfully bought %d shares of %s for $%.2f (Fee: $%.2f)", shares, option, res.getSharePrice(), res.getCommissionPaid()));
            mainController.refreshAllViews();
        } catch (Exception e) {
            mainController.showAlert(Alert.AlertType.ERROR, "Purchase Error", e.getMessage());
        }
    }
}
