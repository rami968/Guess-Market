package engine.service;

import engine.dto.*;
import engine.exception.XmlValidationException;
import engine.model.*;
import engine.xml.XMLDataResult;
import engine.xml.XMLParser;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of EngineService managing events, users, order books, and transactions.
 * Fully thread-safe and stateful across HTTP requests for Exercise 3.
 */
public class EngineServiceImpl implements EngineService {

    private final Map<Integer, GMEvent> eventsMap;
    private final Map<String, GMEvent> eventsByNameMap;
    private final Map<String, User> usersMap;
    private int nextEventId = 1;
    private int version = 1;

    public EngineServiceImpl() {
        this.eventsMap = new LinkedHashMap<>();
        this.eventsByNameMap = new LinkedHashMap<>();
        this.usersMap = new LinkedHashMap<>();
    }

    @Override
    public synchronized int getVersion() {
        return version;
    }

    @Override
    public synchronized void registerUser(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        String key = userName.trim().toLowerCase();
        if (usersMap.containsKey(key)) {
            throw new IllegalArgumentException("Username '" + userName.trim() + "' is already registered.");
        }
        usersMap.put(key, new User(userName.trim()));
        version++;
    }

    @Override
    public synchronized void depositFunds(String userName, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than 0.");
        }
        User user = getUserOrThrow(userName);
        user.deposit(amount, String.format("Account deposit of $%.2f", amount));
        version++;
    }

    @Override
    public synchronized void loadXml(String filePath) {
        XMLDataResult result = XMLParser.parseAndValidate(filePath);
        eventsMap.clear();
        eventsByNameMap.clear();
        usersMap.clear();
        nextEventId = 1;

        for (GMEvent event : result.getEvents()) {
            eventsMap.put(event.getId(), event);
            eventsByNameMap.put(event.getName().toLowerCase(), event);
            if (event.getId() >= nextEventId) {
                nextEventId = event.getId() + 1;
            }
        }
        for (User user : result.getUsers().values()) {
            usersMap.put(user.getName().toLowerCase(), user);
        }
        version++;
    }

    @Override
    public synchronized int loadXmlStream(InputStream inputStream, String uploaderUserName) {
        User uploader = getUserOrThrow(uploaderUserName);
        Set<String> existingNames = new HashSet<>(eventsByNameMap.keySet());
        List<GMEvent> newEvents = XMLParser.parseAndValidate(inputStream, uploader.getName(), existingNames, nextEventId);

        for (GMEvent event : newEvents) {
            eventsMap.put(event.getId(), event);
            eventsByNameMap.put(event.getName().toLowerCase(), event);
            uploader.addMarketMakerEventId(event.getId());
            uploader.addMarketMakerEventName(event.getName());
            if (event.getId() >= nextEventId) {
                nextEventId = event.getId() + 1;
            }
        }
        version++;
        return newEvents.size();
    }

    @Override
    public synchronized boolean isFileLoaded() {
        return !eventsMap.isEmpty();
    }

    @Override
    public synchronized List<EventDTO> getAllEvents() {
        return eventsMap.values().stream()
                .map(this::mapToEventDTO)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<EventDTO> getActiveEvents() {
        return eventsMap.values().stream()
                .filter(e -> e.getStatus() == EventStatus.ACTIVE)
                .map(this::mapToEventDTO)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized EventTradingStatusDTO getEventTradingStatus(int eventId) {
        GMEvent e = getEventOrThrow(eventId);
        return mapToTradingStatusDTO(e);
    }

    @Override
    public synchronized EventTradingStatusDTO getEventTradingStatusByName(String eventName) {
        GMEvent e = getEventByNameOrThrow(eventName);
        return mapToTradingStatusDTO(e);
    }

    private EventTradingStatusDTO mapToTradingStatusDTO(GMEvent e) {
        List<TransactionDTO> historyDTO = new ArrayList<>();
        for (Transaction tx : e.getTransactionHistory()) {
            historyDTO.add(new TransactionDTO(tx.getOptionName(), tx.getShareCount(), tx.getPricePaid()));
        }
        String winningName = null;
        if (e.getWinningOptionIndex() == 1) winningName = e.getOption1();
        else if (e.getWinningOptionIndex() == 2) winningName = e.getOption2();

        return new EventTradingStatusDTO(
                e.getId(), e.getName(), e.getStatus(), e.getOption1(), e.getOption2(),
                e.getProbabilityOption1(), e.getProbabilityOption2(),
                e.getQ1(), e.getQ2(),
                e.getEventAccountBalance(), e.getTotalCommissionCollected(),
                e.getWinningOptionIndex(), winningName, historyDTO
        );
    }

    @Override
    public synchronized PurchaseResultDTO buyShares(int eventId, int optionIndex, int shareCount) {
        validateFileLoaded();
        if (usersMap.isEmpty()) {
            throw new IllegalStateException("No users in the system.");
        }
        String firstUser = usersMap.keySet().iterator().next();
        return buySharesLMSR(eventId, firstUser, optionIndex, shareCount);
    }

    @Override
    public synchronized EventTradingStatusDTO closeEvent(int eventId, int winningOptionIndex) {
        GMEvent e = getEventOrThrow(eventId);
        String mmUser = e.getMarketMakerName();
        if (mmUser.isEmpty() && !usersMap.isEmpty()) {
            mmUser = usersMap.keySet().iterator().next();
        }
        closeEvent(eventId, mmUser, winningOptionIndex);
        return getEventTradingStatus(eventId);
    }

    @Override
    public synchronized List<EventDTO> getFilteredEvents(String typeFilter, String statusFilter, String commissionFilter) {
        return eventsMap.values().stream()
                .filter(e -> matchesTypeFilter(e, typeFilter))
                .filter(e -> matchesStatusFilter(e, statusFilter))
                .filter(e -> matchesCommissionFilter(e, commissionFilter))
                .map(this::mapToEventDTO)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized EventDTO getEventDetails(int eventId) {
        GMEvent event = getEventOrThrow(eventId);
        return mapToEventDTO(event);
    }

    @Override
    public synchronized EventDTO getEventDetailsByName(String eventName) {
        GMEvent event = getEventByNameOrThrow(eventName);
        return mapToEventDTO(event);
    }

    @Override
    public synchronized List<UserDTO> getAllUsers() {
        return usersMap.values().stream()
                .map(this::mapToUserDTO)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<UserSummaryDTO> getAllUserSummaries() {
        return usersMap.values().stream()
                .map(u -> new UserSummaryDTO(u.getName(), u.getBalance(), u.isMarketMaker()))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized UserDTO getUserDetails(String userName) {
        User user = getUserOrThrow(userName);
        return mapToUserDTO(user);
    }

    @Override
    public synchronized void activateEvent(int eventId, String mmUserName) {
        GMEvent event = getEventOrThrow(eventId);
        User mmUser = getUserOrThrow(mmUserName);
        event.activateEvent(mmUser);
        version++;
    }

    @Override
    public synchronized void activateEventByName(String eventName, String mmUserName) {
        GMEvent event = getEventByNameOrThrow(eventName);
        User mmUser = getUserOrThrow(mmUserName);
        event.activateEvent(mmUser);
        version++;
    }

    @Override
    public synchronized PurchaseResultDTO buySharesLMSR(int eventId, String userName, int optionIndex, int shareCount) {
        GMEvent event = getEventOrThrow(eventId);
        User user = getUserOrThrow(userName);
        GMEvent.PurchaseResult result = event.buySharesLMSR(user, optionIndex, shareCount);

        EventTradingStatusDTO updatedStatus = getEventTradingStatus(eventId);
        version++;
        return new PurchaseResultDTO(result.getSharePrice(), result.getCommissionPaid(), updatedStatus);
    }

    @Override
    public synchronized PurchaseResultDTO buySharesLMSRByName(String eventName, String userName, int optionIndex, int shareCount) {
        GMEvent event = getEventByNameOrThrow(eventName);
        User user = getUserOrThrow(userName);
        GMEvent.PurchaseResult result = event.buySharesLMSR(user, optionIndex, shareCount);

        EventTradingStatusDTO updatedStatus = mapToTradingStatusDTO(event);
        version++;
        return new PurchaseResultDTO(result.getSharePrice(), result.getCommissionPaid(), updatedStatus);
    }

    @Override
    public synchronized void submitOrder(int eventId, String userName, String optionName, String orderTypeStr, int shareCount, double pricePerShare) {
        GMEvent event = getEventOrThrow(eventId);
        User user = getUserOrThrow(userName);
        OrderType orderType = OrderType.valueOf(orderTypeStr.toUpperCase());
        Order order = new Order(user.getName(), optionName, orderType, shareCount, pricePerShare, event.getD());

        User mmUser = usersMap.get(event.getMarketMakerName().toLowerCase());
        event.submitOrder(order, usersMap, mmUser);
        version++;
    }

    @Override
    public synchronized void submitOrderByName(String eventName, String userName, String optionName, String orderTypeStr, int shareCount, double pricePerShare) {
        GMEvent event = getEventByNameOrThrow(eventName);
        User user = getUserOrThrow(userName);
        OrderType orderType = OrderType.valueOf(orderTypeStr.toUpperCase());
        Order order = new Order(user.getName(), optionName, orderType, shareCount, pricePerShare, event.getD());

        User mmUser = usersMap.get(event.getMarketMakerName().toLowerCase());
        event.submitOrder(order, usersMap, mmUser);
        version++;
    }

    @Override
    public synchronized EventDTO closeEvent(int eventId, String mmUserName, int winningOptionIndex) {
        GMEvent event = getEventOrThrow(eventId);
        User mmUser = getUserOrThrow(mmUserName);

        event.closeEvent(mmUser, winningOptionIndex, usersMap);
        version++;
        return mapToEventDTO(event);
    }

    @Override
    public synchronized EventDTO closeEventByName(String eventName, String mmUserName, int winningOptionIndex) {
        GMEvent event = getEventByNameOrThrow(eventName);
        User mmUser = getUserOrThrow(mmUserName);

        event.closeEvent(mmUser, winningOptionIndex, usersMap);
        version++;
        return mapToEventDTO(event);
    }

    @Override
    public synchronized void saveSystemState(String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(new ArrayList<>(eventsMap.values()));
            oos.writeObject(new ArrayList<>(usersMap.values()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save system state: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void loadSystemState(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            List<GMEvent> events = (List<GMEvent>) ois.readObject();
            List<User> users = (List<User>) ois.readObject();

            eventsMap.clear();
            eventsByNameMap.clear();
            usersMap.clear();
            nextEventId = 1;

            for (GMEvent e : events) {
                eventsMap.put(e.getId(), e);
                eventsByNameMap.put(e.getName().toLowerCase(), e);
                if (e.getId() >= nextEventId) {
                    nextEventId = e.getId() + 1;
                }
            }
            for (User u : users) {
                usersMap.put(u.getName().toLowerCase(), u);
            }
            version++;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load system state: " + e.getMessage(), e);
        }
    }

    private void validateFileLoaded() {
        if (!isFileLoaded()) {
            throw new IllegalStateException("No events loaded in the system.");
        }
    }

    private GMEvent getEventOrThrow(int eventId) {
        GMEvent event = eventsMap.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event with ID #" + eventId + " not found.");
        }
        return event;
    }

    private GMEvent getEventByNameOrThrow(String eventName) {
        if (eventName == null) throw new IllegalArgumentException("Event name cannot be null.");
        GMEvent event = eventsByNameMap.get(eventName.trim().toLowerCase());
        if (event == null) {
            throw new IllegalArgumentException("Event with name '" + eventName.trim() + "' not found.");
        }
        return event;
    }

    private User getUserOrThrow(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        User user = usersMap.get(userName.trim().toLowerCase());
        if (user == null) {
            throw new IllegalArgumentException("User '" + userName + "' does not exist.");
        }
        return user;
    }

    private boolean matchesTypeFilter(GMEvent e, String filter) {
        if (filter == null || filter.equalsIgnoreCase("ALL") || filter.isEmpty()) return true;
        return e.getEventType().name().equalsIgnoreCase(filter.trim());
    }

    private boolean matchesStatusFilter(GMEvent e, String filter) {
        if (filter == null || filter.equalsIgnoreCase("ALL") || filter.isEmpty()) return true;
        return e.getStatus().name().equalsIgnoreCase(filter.trim());
    }

    private boolean matchesCommissionFilter(GMEvent e, String filter) {
        if (filter == null || filter.equalsIgnoreCase("ALL") || filter.isEmpty()) return true;
        return e.getCommissionType().name().equalsIgnoreCase(filter.trim());
    }

    private EventDTO mapToEventDTO(GMEvent e) {
        OrderBookStatsDTO statsOpt1 = mapToOrderBookStatsDTO(e.getOrderBookOption1());
        OrderBookStatsDTO statsOpt2 = mapToOrderBookStatsDTO(e.getOrderBookOption2());

        return new EventDTO(
                e.getId(), e.getName(), e.getDescription(), e.getCommission(), e.getCommissionType(),
                e.getOption1(), e.getOption2(), e.getEventType(), e.getMarketMakerName(), e.getStatus(),
                e.getEventAccountBalance(), e.getTotalCommissionCollected(),
                e.getProbabilityOption1(), e.getProbabilityOption2(),
                statsOpt1, statsOpt2
        );
    }

    private OrderBookStatsDTO mapToOrderBookStatsDTO(OrderBook ob) {
        if (ob == null) return null;

        List<OrderDTO> buyDTOs = ob.getBuyOrders().stream()
                .map(o -> new OrderDTO(o.getId(), o.getUserName(), o.getOptionName(), o.getType().name(), o.getSharesCount(), o.getPricePerShare(), o.getTimestamp()))
                .collect(Collectors.toList());

        List<OrderDTO> sellDTOs = ob.getSellOrders().stream()
                .map(o -> new OrderDTO(o.getId(), o.getUserName(), o.getOptionName(), o.getType().name(), o.getSharesCount(), o.getPricePerShare(), o.getTimestamp()))
                .collect(Collectors.toList());

        return new OrderBookStatsDTO(
                ob.getOptionName(), ob.getLastPrice(), ob.getHighestBid(), ob.getLowestAsk(),
                ob.getMidPrice(), ob.getSpread(), buyDTOs, sellDTOs
        );
    }

    private UserDTO mapToUserDTO(User u) {
        List<UserDTO.UserTransactionDTO> txDTOs = u.getTransactionHistory().stream()
                .map(t -> new UserDTO.UserTransactionDTO(t.getDescription(), t.getAmount(), t.getBalanceAfter(), t.getTimestamp()))
                .collect(Collectors.toList());

        return new UserDTO(
                u.getName(), u.getBalance(), u.isBlocked(), u.isMarketMaker(),
                u.getMarketMakerEventIds(), u.getMarketMakerEventNames(),
                u.getAllHoldings(), u.getAllHoldingsByName(), txDTOs
        );
    }
}
