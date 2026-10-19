package engine.service;

import engine.dto.*;
import java.io.InputStream;
import java.util.List;

/**
 * Main service contract exposed by the Engine module.
 * Completely passive and thread-safe, returning DTOs and accepting operations.
 */
public interface EngineService {

    /**
     * Loads and validates an XML file from file path (Exercise 1/2 backwards compatibility).
     */
    void loadXml(String filePath);

    /**
     * Loads an XML input stream uploaded by a specific user (Exercise 3).
     * Events in the stream are accumulated and associated with the uploader as Market Maker.
     * @return count of newly added events.
     */
    int loadXmlStream(InputStream inputStream, String uploaderUserName);

    /**
     * Returns true if any events are loaded in the system.
     */
    boolean isFileLoaded();

    /**
     * Version counter for Polling optimization (increments on every state-changing event).
     */
    int getVersion();

    /**
     * Registers a new user in the system.
     */
    void registerUser(String userName);

    /**
     * Deposits funds into a user's account.
     */
    void depositFunds(String userName, double amount);

    /**
     * Returns a list of all loaded events.
     */
    List<EventDTO> getAllEvents();

    /**
     * Returns list of active events only.
     */
    List<EventDTO> getActiveEvents();

    /**
     * Returns full trading status for selected event ID.
     */
    EventTradingStatusDTO getEventTradingStatus(int eventId);

    /**
     * Returns full trading status for selected event Name.
     */
    EventTradingStatusDTO getEventTradingStatusByName(String eventName);

    /**
     * Buys shares in an active LMSR event (backwards compatibility).
     */
    PurchaseResultDTO buyShares(int eventId, int optionIndex, int shareCount);

    /**
     * Closes an active event (backwards compatibility).
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
     * Returns event DTO for a specific event Name.
     */
    EventDTO getEventDetailsByName(String eventName);

    /**
     * Returns list of all defined users with full details.
     */
    List<UserDTO> getAllUsers();

    /**
     * Returns lightweight user summaries (name, balance, isMarketMaker) for other users table.
     */
    List<UserSummaryDTO> getAllUserSummaries();

    /**
     * Returns user DTO for a specific user name.
     */
    UserDTO getUserDetails(String userName);

    /**
     * Activates an inactive event (performed by event's Market Maker).
     */
    void activateEvent(int eventId, String mmUserName);

    /**
     * Activates an inactive event by event name (performed by event's Market Maker).
     */
    void activateEventByName(String eventName, String mmUserName);

    /**
     * Buys LMSR shares in an active event.
     */
    PurchaseResultDTO buySharesLMSR(int eventId, String userName, int optionIndex, int shareCount);

    /**
     * Buys LMSR shares in an active event by event name.
     */
    PurchaseResultDTO buySharesLMSRByName(String eventName, String userName, int optionIndex, int shareCount);

    /**
     * Submits a Buy or Sell Order to an Order Book event.
     */
    void submitOrder(int eventId, String userName, String optionName, String orderTypeStr, int shareCount, double pricePerShare);

    /**
     * Submits a Buy or Sell Order to an Order Book event by event name.
     */
    void submitOrderByName(String eventName, String userName, String optionName, String orderTypeStr, int shareCount, double pricePerShare);

    /**
     * Closes an active event and determines the winner (performed by event's Market Maker).
     */
    EventDTO closeEvent(int eventId, String mmUserName, int winningOptionIndex);

    /**
     * Closes an active event and determines the winner by event name (performed by event's Market Maker).
     */
    EventDTO closeEventByName(String eventName, String mmUserName, int winningOptionIndex);

    /**
     * Saves system state to binary file.
     */
    void saveSystemState(String filePath);

    /**
     * Loads system state from binary file.
     */
    void loadSystemState(String filePath);
}
