package ui;

import engine.dto.*;
import engine.exception.XMLException;
import engine.model.EventStatus;
import engine.service.EngineService;

import java.util.List;
import java.util.Scanner;

// Handles console interface, menu loop, input validation, and output formatting.
public class ConsoleUI {

    private final EngineService engine;
    private final Scanner scanner;

    public ConsoleUI(EngineService engine) {
        this.engine = engine;
        this.scanner = new Scanner(System.in);
    }

    // Runs main menu application loop.
    public void run() {
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("Select an option (1-8): ");
            System.out.println();

            switch (choice) {
                case 1:
                    handleLoadXml();
                    break;
                case 2:
                    handleDisplayAllEvents();
                    break;
                case 3:
                    handleDisplayEventStatus();
                    break;
                case 4:
                    handleParticipateInEvent();
                    break;
                case 5:
                    handleCloseEvent();
                    break;
                case 6:
                    handleSaveState();
                    break;
                case 7:
                    handleLoadState();
                    break;
                case 8:
                    running = false;
                    System.out.println("Exiting Guess Market. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option. Please select a number between 1 and 8.");
            }
            System.out.println();
        }
    }

    private void printMainMenu() {
        System.out.println("========== GUESS MARKET MAIN MENU ==========");
        System.out.println("1. Load System XML File");
        System.out.println("2. Display All Events");
        System.out.println("3. Display Event Trading Status");
        System.out.println("4. Participate in Event (Buy Shares)");
        System.out.println("5. Close Event");
        System.out.println("6. Save System State");
        System.out.println("7. Load System State");
        System.out.println("8. Exit");
        System.out.println("============================================");
    }

    private void handleLoadXml() {
        System.out.print("Enter full path to XML file: ");
        String filePath = scanner.nextLine().trim();

        try {
            engine.loadXml(filePath);
            System.out.println("SUCCESS: XML file loaded successfully!");
        } catch (XMLException e) {
            System.out.println("ERROR: Failed to load XML file.");
            System.out.println("Reason: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("ERROR: An unexpected error occurred while loading XML file.");
            System.out.println("Details: " + e.getMessage());
        }
    }

    private void handleDisplayAllEvents() {
        if (!validateFileLoaded()) return;

        try {
            List<EventDTO> events = engine.getAllEvents();
            System.out.println("--- ALL LOADED EVENTS (" + events.size() + ") ---");
            for (EventDTO e : events) {
                printEventSummary(e);
                System.out.println("----------------------------------------");
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private void handleDisplayEventStatus() {
        if (!validateFileLoaded()) return;

        List<EventDTO> events = engine.getAllEvents();
        if (events.isEmpty()) {
            System.out.println("No events currently exist in system.");
            return;
        }

        EventDTO selected = selectEventFromList(events, "Select an event to view status:");
        if (selected == null) return;

        try {
            EventTradingStatusDTO status = engine.getEventTradingStatus(selected.getId());
            printEventTradingStatus(status);
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private void handleParticipateInEvent() {
        if (!validateFileLoaded()) return;

        List<EventDTO> activeEvents = engine.getActiveEvents();
        if (activeEvents.isEmpty()) {
            System.out.println("No ACTIVE events available for trading.");
            return;
        }

        EventDTO selected = selectEventFromList(activeEvents, "Select an active event to trade:");
        if (selected == null) return;

        try {
            EventTradingStatusDTO currentStatus = engine.getEventTradingStatus(selected.getId());
            System.out.println("\n--- Current Event Trading Status ---");
            printEventTradingStatus(currentStatus);

            System.out.println("\nOptions to buy:");
            System.out.println("1. " + selected.getOption1());
            System.out.println("2. " + selected.getOption2());

            int optionIndex = readInt("Select option (1 or 2): ");
            if (optionIndex != 1 && optionIndex != 2) {
                System.out.println("Invalid option selection. Purchase cancelled.");
                return;
            }

            int shareCount = readInt("Enter number of shares to purchase: ");
            if (shareCount <= 0) {
                System.out.println("Share count must be positive. Purchase cancelled.");
                return;
            }

            PurchaseResultDTO result = engine.buyShares(selected.getId(), optionIndex, shareCount);

            System.out.println("\n--- PURCHASE RECEIPT ---");
            System.out.println("Shares Price:        $" + formatTwoDecimals(result.getSharePrice()));
            System.out.println("Commission Paid:     $" + formatTwoDecimals(result.getCommissionPaid()));
            System.out.println("Total Amount Paid:   $" + formatTwoDecimals(result.getTotalPaid()));

            System.out.println("\n--- UPDATED TRADING STATUS ---");
            printEventTradingStatus(result.getUpdatedStatus());

        } catch (Exception e) {
            System.out.println("ERROR: Purchase failed - " + e.getMessage());
        }
    }

    private void handleCloseEvent() {
        if (!validateFileLoaded()) return;

        List<EventDTO> activeEvents = engine.getActiveEvents();
        if (activeEvents.isEmpty()) {
            System.out.println("No ACTIVE events available for closing.");
            return;
        }

        EventDTO selected = selectEventFromList(activeEvents, "Select an active event to close:");
        if (selected == null) return;

        try {
            EventTradingStatusDTO currentStatus = engine.getEventTradingStatus(selected.getId());
            System.out.println("\n--- Current Event Details ---");
            printEventTradingStatus(currentStatus);

            System.out.println("\nSelect the WINNING option:");
            System.out.println("1. " + selected.getOption1());
            System.out.println("2. " + selected.getOption2());

            int winningIndex = readInt("Select winning option (1 or 2): ");
            if (winningIndex != 1 && winningIndex != 2) {
                System.out.println("Invalid selection. Event closing cancelled.");
                return;
            }

            EventTradingStatusDTO closedStatus = engine.closeEvent(selected.getId(), winningIndex);

            System.out.println("\n--- EVENT CLOSED SUMMARY ---");
            printEventTradingStatus(closedStatus);

        } catch (Exception e) {
            System.out.println("ERROR: Failed to close event - " + e.getMessage());
        }
    }

    private void handleSaveState() {
        if (!validateFileLoaded()) return;

        System.out.print("Enter full path or filename to save system state (without extension): ");
        String filePath = scanner.nextLine().trim();
        if (filePath.isEmpty()) {
            System.out.println("Filename cannot be empty. Save cancelled.");
            return;
        }

        if (!filePath.toLowerCase().endsWith(".dat")) {
            filePath += ".dat";
        }

        try {
            engine.saveSystemState(filePath);
            System.out.println("SUCCESS: System state saved successfully to '" + filePath + "'!");
        } catch (Exception e) {
            System.out.println("ERROR: Failed to save system state - " + e.getMessage());
        }
    }

    private void handleLoadState() {
        System.out.print("Enter full path or filename of saved system state (without extension): ");
        String filePath = scanner.nextLine().trim();
        if (filePath.isEmpty()) {
            System.out.println("Filename cannot be empty. Load cancelled.");
            return;
        }

        if (!filePath.toLowerCase().endsWith(".dat")) {
            filePath += ".dat";
        }

        try {
            engine.loadSystemState(filePath);
            System.out.println("SUCCESS: System state loaded successfully from '" + filePath + "'!");
            System.out.println("Loaded " + engine.getAllEvents().size() + " active/historical event(s).");
        } catch (Exception e) {
            System.out.println("ERROR: Failed to load system state - " + e.getMessage());
        }
    }

    private boolean validateFileLoaded() {
        if (!engine.isFileLoaded()) {
            System.out.println("ERROR: No valid XML file is loaded in the system.");
            System.out.println("Please run Option 1 (Load System XML File) first.");
            return false;
        }
        return true;
    }

    private EventDTO selectEventFromList(List<EventDTO> eventsList, String promptHeader) {
        System.out.println("\n" + promptHeader);
        for (int i = 0; i < eventsList.size(); i++) {
            EventDTO e = eventsList.get(i);
            System.out.println((i + 1) + ". [ID: " + e.getId() + "] " + e.getName() + " (" + e.getStatus() + ")");
        }

        int selection = readInt("Enter choice number (1-" + eventsList.size() + "): ");
        if (selection < 1 || selection > eventsList.size()) {
            System.out.println("Invalid selection index.");
            return null;
        }

        return eventsList.get(selection - 1);
    }

    private void printEventSummary(EventDTO e) {
        System.out.println("Event ID:        " + e.getId());
        System.out.println("Name:            " + e.getName());
        System.out.println("Description:     " + e.getDescription());
        System.out.println("Commission:      " + e.getCommission() + "%");
        System.out.println("Commission Type: " + e.getCommissionType());
        System.out.println("Options:         1) " + e.getOption1() + "  |  2) " + e.getOption2());
        System.out.println("Status:          " + e.getStatus());
    }

    private void printEventTradingStatus(EventTradingStatusDTO s) {
        System.out.println("Event ID:               " + s.getEventId());
        System.out.println("Event Name:             " + s.getEventName());
        System.out.println("Status:                 " + s.getStatus());
        System.out.println("Option 1 (" + s.getOption1() + "):   Value/Prob: " + formatTwoDecimals(s.getProb1()) + " | Shares Bought: " + s.getQ1());
        System.out.println("Option 2 (" + s.getOption2() + "):   Value/Prob: " + formatTwoDecimals(s.getProb2()) + " | Shares Bought: " + s.getQ2());
        System.out.println("Event Account Balance:  $" + formatTwoDecimals(s.getEventAccountBalance()));
        System.out.println("Total Commission:       $" + formatTwoDecimals(s.getTotalCommissionCollected()));

        if (s.getStatus() == EventStatus.CLOSED) {
            System.out.println("Winning Option:         " + s.getWinningOptionName() + " (Option #" + s.getWinningOptionIndex() + ")");
        }

        System.out.println("\nTrade History (Newest First):");
        List<TransactionDTO> history = s.getTransactionHistory();
        if (history.isEmpty()) {
            System.out.println("  [No trades executed yet]");
        } else {
            for (int i = 0; i < history.size(); i++) {
                TransactionDTO tx = history.get(i);
                System.out.println("  " + (i + 1) + ". Bought " + tx.getShareCount() + " shares of '" + tx.getOptionName() + "' for $" + formatTwoDecimals(tx.getPricePaid()));
            }
        }
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid integer number.");
            }
        }
    }

    private String formatTwoDecimals(double val) {
        return String.format("%.2f", val);
    }
}
