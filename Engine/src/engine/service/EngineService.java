package engine.service;

import engine.dto.*;
import java.util.List;

/**
 * Main service contract exposed by the Engine module to the UI.
 * Completely passive, returning DTOs and accepting operations.
 */
public interface EngineService {

    /**
     * Loads and validates an XML file (Schema v1 and v2).
     */
    void loadXml(String filePath);

    /**
     * Returns true if a valid file is currently loaded.
     */
    boolean isFileLoaded();

    /**
     * Returns a list of all loaded events.
     */
    List<EventDTO> getAllEvents();

    /**
     * Returns list of active events only (for backwards compatibility).
     */
    List<EventDTO> getActiveEvents();

    /**
     * Returns full trading status for selected event ID (for backwards compatibility).
     */
    EventTradingStatusDTO getEventTradingStatus(int eventId);

    /**
     * Buys shares in an active LMSR event (for backwards compatibility).
     */
    PurchaseResultDTO buyShares(int eventId, int optionIndex, int shareCount);

    /**
     * Closes an active event (for backwards compatibility).
     */
    EventTradingStatusDTO closeEvent(int eventId, int winningOptionIndex);

    /**
     * Returns a list of events filtered by type (LMSR/OB), status (INACTIVE/ACTIVE/CLOSED), and commission type.
     */
    List<EventDTO> getFilteredEvents(String typeFilter, String statusFilter, String commissionFilter);

    /**
     * Returns event DTO for a specific event ID.
     */
    EventDTO getEventDetails(int eventId);

    /**
     * Returns list of all defined users.
     */
    List<UserDTO> getAllUsers();

    /**
     * Returns user DTO for a specific user name.
     */
    UserDTO getUserDetails(String userName);

    /**
     * Activates an inactive event (performed by event's Market Maker).
     */
    void activateEvent(int eventId, String mmUserName);

    /**
     * Buys LMSR shares in an active event.
     */
    PurchaseResultDTO buySharesLMSR(int eventId, String userName, int optionIndex, int shareCount);

    /**
     * Submits a Buy or Sell Order to an Order Book event.
     */
    void submitOrder(int eventId, String userName, String optionName, String orderTypeStr, int shareCount, double pricePerShare);

    /**
     * Closes an active event and determines the winner (performed by event's Market Maker).
     */
    EventDTO closeEvent(int eventId, String mmUserName, int winningOptionIndex);

    /**
     * Saves system state to binary file.
     */
    void saveSystemState(String filePath);

    /**
     * Loads system state from binary file.
     */
    void loadSystemState(String filePath);
}
