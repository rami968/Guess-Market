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
 */
public class EngineServiceImpl implements EngineService {

    private final Map<Integer, GMEvent> eventsMap;
    private final Map<String, User> usersMap;

    public EngineServiceImpl() {
        this.eventsMap = new LinkedHashMap<>();
        this.usersMap = new LinkedHashMap<>();
    }

    @Override
    public synchronized void loadXml(String filePath) {
        XMLDataResult result = XMLParser.parseAndValidate(filePath);
        eventsMap.clear();
        usersMap.clear();

        for (GMEvent event : result.getEvents()) {
            eventsMap.put(event.getId(), event);
        }
        for (User user : result.getUsers().values()) {
            usersMap.put(user.getName().toLowerCase(), user);
        }
    }

    @Override
    public boolean isFileLoaded() {
        return !eventsMap.isEmpty();
    }

    @Override
    public List<EventDTO> getAllEvents() {
        validateFileLoaded();
        return eventsMap.values().stream()
                .map(this::mapToEventDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventDTO> getActiveEvents() {
        validateFileLoaded();
        return eventsMap.values().stream()
                .filter(e -> e.getStatus() == EventStatus.ACTIVE)
                .map(this::mapToEventDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EventTradingStatusDTO getEventTradingStatus(int eventId) {
        validateFileLoaded();
        GMEvent e = getEventOrThrow(eventId);
        List<TransactionDTO> historyDTO = new ArrayList<>();
        for (Transaction tx : e.getTransactionHistory()) {
            historyDTO.add(new TransactionDTO(tx.getOptionName(), tx.getShareCount(), tx.getPricePaid()));
        }
        String winningName = null;
        if (e.getStatus() == EventStatus.CLOSED) {
            winningName = (e.getWinningOptionIndex() == 1) ? e.getOption1() : e.getOption2();
        }
        return new EventTradingStatusDTO(
                e.getId(), e.getName(), e.getStatus(), e.getOption1(), e.getOption2(),
                e.getProbabilityOption1(), e.getProbabilityOption2(), e.getQ1(), e.getQ2(),
                e.getEventAccountBalance(), e.getTotalCommissionCollected(), e.getWinningOptionIndex(),
                winningName, historyDTO
        );
    }

    @Override
    public PurchaseResultDTO buyShares(int eventId, int optionIndex, int shareCount) {
        String firstUser = usersMap.isEmpty() ? "DefaultUser" : usersMap.keySet().iterator().next();
        return buySharesLMSR(eventId, firstUser, optionIndex, shareCount);
    }

    @Override
    public EventTradingStatusDTO closeEvent(int eventId, int winningOptionIndex) {
        GMEvent e = getEventOrThrow(eventId);
        String mmUser = e.getMarketMakerName();
        if (mmUser.isEmpty() && !usersMap.isEmpty()) {
            mmUser = usersMap.keySet().iterator().next();
        }
        closeEvent(eventId, mmUser, winningOptionIndex);
        return getEventTradingStatus(eventId);
    }

    @Override
    public List<EventDTO> getFilteredEvents(String typeFilter, String statusFilter, String commissionFilter) {
        validateFileLoaded();
        return eventsMap.values().stream()
                .filter(e -> matchesTypeFilter(e, typeFilter))
                .filter(e -> matchesStatusFilter(e, statusFilter))
                .filter(e -> matchesCommissionFilter(e, commissionFilter))
                .map(this::mapToEventDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EventDTO getEventDetails(int eventId) {
        validateFileLoaded();
        GMEvent event = getEventOrThrow(eventId);
        return mapToEventDTO(event);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        validateFileLoaded();
        return usersMap.values().stream()
                .map(this::mapToUserDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO getUserDetails(String userName) {
        validateFileLoaded();
        User user = getUserOrThrow(userName);
        return mapToUserDTO(user);
    }

    @Override
    public synchronized void activateEvent(int eventId, String mmUserName) {
        validateFileLoaded();
        GMEvent event = getEventOrThrow(eventId);
        User mmUser = getUserOrThrow(mmUserName);
        event.activateEvent(mmUser);
    }

    @Override
    public synchronized PurchaseResultDTO buySharesLMSR(int eventId, String userName, int optionIndex, int shareCount) {
        validateFileLoaded();
        GMEvent event = getEventOrThrow(eventId);
        User user = getUserOrThrow(userName);

        GMEvent.PurchaseResult result = event.buySharesLMSR(user, optionIndex, shareCount);

        List<TransactionDTO> historyDTO = new ArrayList<>();
        for (Transaction tx : event.getTransactionHistory()) {
            historyDTO.add(new TransactionDTO(tx.getOptionName(), tx.getShareCount(), tx.getPricePaid()));
        }

        EventTradingStatusDTO updatedStatus = new EventTradingStatusDTO(
                event.getId(), event.getName(), event.getStatus(), event.getOption1(), event.getOption2(),
                event.getProbabilityOption1(), event.getProbabilityOption2(), event.getQ1(), event.getQ2(),
                event.getEventAccountBalance(), event.getTotalCommissionCollected(), event.getWinningOptionIndex(),
                null, historyDTO
        );

        return new PurchaseResultDTO(result.getSharePrice(), result.getCommissionPaid(), updatedStatus);
    }

    @Override
    public synchronized void submitOrder(int eventId, String userName, String optionName, String orderTypeStr, int shareCount, double pricePerShare) {
        validateFileLoaded();
        GMEvent event = getEventOrThrow(eventId);
        User user = getUserOrThrow(userName);

        OrderType orderType = OrderType.valueOf(orderTypeStr.trim().toUpperCase());
        Order order = new Order(user.getName(), optionName, orderType, shareCount, pricePerShare, event.getD());

        User mmUser = usersMap.get(event.getMarketMakerName().toLowerCase());
        event.submitOrder(order, usersMap, mmUser);
    }

    @Override
    public synchronized EventDTO closeEvent(int eventId, String mmUserName, int winningOptionIndex) {
        validateFileLoaded();
        GMEvent event = getEventOrThrow(eventId);
        User mmUser = getUserOrThrow(mmUserName);

        event.closeEvent(mmUser, winningOptionIndex, usersMap);
        return mapToEventDTO(event);
    }

    @Override
    public void saveSystemState(String filePath) {
        validateFileLoaded();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(new ArrayList<>(eventsMap.values()));
            oos.writeObject(new ArrayList<>(usersMap.values()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save system state: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void loadSystemState(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Saved state file not found at path: " + filePath);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            List<GMEvent> loadedEvents = (List<GMEvent>) ois.readObject();
            List<User> loadedUsers = (List<User>) ois.readObject();

            eventsMap.clear();
            usersMap.clear();

            for (GMEvent e : loadedEvents) {
                eventsMap.put(e.getId(), e);
            }
            for (User u : loadedUsers) {
                usersMap.put(u.getName().toLowerCase(), u);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load system state from file: " + e.getMessage(), e);
        }
    }

    private void validateFileLoaded() {
        if (!isFileLoaded()) {
            throw new XmlValidationException("No valid XML file is currently loaded in system.");
        }
    }

    private GMEvent getEventOrThrow(int eventId) {
        GMEvent event = eventsMap.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event with ID " + eventId + " does not exist.");
        }
        return event;
    }

    private User getUserOrThrow(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be empty.");
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
                u.getName(), u.getBalance(), u.isBlocked(), u.getMarketMakerEventIds(),
                u.getAllHoldings(), txDTOs
        );
    }
}
