package engine.model;

import java.io.Serializable;
import java.util.*;

/**
 * Represents a user in the Guess Market application.
 * Users can be regular traders or Market Makers (MM) for specific events.
 * Keeps track of account balance, active holdings per event option, and transactions.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 3L;
    private final String name;
    private double balance;
    private boolean isBlocked;
    private final Set<Integer> marketMakerEventIds;
    private final Set<String> marketMakerEventNames;
    
    // Tracks share holdings per event and option: Map<EventID, Map<OptionName, ShareCount>>
    private final Map<Integer, Map<String, Integer>> holdingsPerEvent;
    private final Map<String, Map<String, Integer>> holdingsPerEventName;
    
    // Tracks total amount paid per event option for profit/loss calculation
    private final Map<Integer, Map<String, Double>> investmentPerEvent;
    private final Map<String, Map<String, Double>> investmentPerEventName;

    // List of account balance transactions history
    private final List<UserTransaction> transactionHistory;

    public User(String name) {
        this(name, 0.0);
    }

    public User(String name, double initialCash) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be null or empty");
        }
        if (initialCash < 0) {
            throw new IllegalArgumentException("Initial balance for user cannot be negative");
        }
        this.name = name.trim();
        this.balance = initialCash;
        this.isBlocked = false;
        this.marketMakerEventIds = new HashSet<>();
        this.marketMakerEventNames = new HashSet<>();
        this.holdingsPerEvent = new HashMap<>();
        this.holdingsPerEventName = new HashMap<>();
        this.investmentPerEvent = new HashMap<>();
        this.investmentPerEventName = new HashMap<>();
        this.transactionHistory = new ArrayList<>();

        if (initialCash > 0) {
            addTransaction(new UserTransaction("Initial Deposit", initialCash, balance));
        } else {
            addTransaction(new UserTransaction("Account Registered", 0.0, balance));
        }
    }

    public String getName() {
        return name;
    }

    public synchronized double getBalance() {
        return balance;
    }

    public synchronized boolean isBlocked() {
        return isBlocked;
    }

    public synchronized void setBlocked(boolean blocked) {
        this.isBlocked = blocked;
    }

    public synchronized boolean isMarketMaker() {
        return !marketMakerEventIds.isEmpty() || !marketMakerEventNames.isEmpty();
    }

    public synchronized Set<Integer> getMarketMakerEventIds() {
        return Collections.unmodifiableSet(marketMakerEventIds);
    }

    public synchronized Set<String> getMarketMakerEventNames() {
        return Collections.unmodifiableSet(marketMakerEventNames);
    }

    public synchronized void addMarketMakerEventId(int eventId) {
        marketMakerEventIds.add(eventId);
    }

    public synchronized void addMarketMakerEventName(String eventName) {
        if (eventName != null) {
            marketMakerEventNames.add(eventName.trim().toLowerCase());
        }
    }

    public synchronized boolean isMarketMakerFor(int eventId) {
        return marketMakerEventIds.contains(eventId);
    }

    public synchronized boolean isMarketMakerFor(String eventName) {
        return eventName != null && marketMakerEventNames.contains(eventName.trim().toLowerCase());
    }

    /**
     * Records submitting an Order Book order into user transaction history.
     */
    public synchronized void recordOrderSubmission(int eventId, String optionName, String orderType, int shareCount, double pricePerShare) {
        String desc = String.format("Submitted %s order for %d shares of %s @ $%.2f in event #%d",
                orderType, shareCount, optionName, pricePerShare, eventId);
        addTransaction(new UserTransaction(desc, 0.0, balance));
    }

    public synchronized void recordOrderSubmission(String eventName, String optionName, String orderType, int shareCount, double pricePerShare) {
        String desc = String.format("Submitted %s order for %d shares of %s @ $%.2f in event '%s'",
                orderType, shareCount, optionName, pricePerShare, eventName);
        addTransaction(new UserTransaction(desc, 0.0, balance));
    }

    /**
     * Deposits money into the user's account.
     */
    public synchronized void deposit(double amount, String reason) {
        if (isBlocked) {
            throw new IllegalStateException("User " + name + " is blocked and cannot perform transactions.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Deposit amount cannot be negative.");
        }
        this.balance += amount;
        addTransaction(new UserTransaction(reason, amount, balance));
    }

    /**
     * Withdraws / charges money from user account.
     */
    public synchronized void withdraw(double amount, String reason) {
        if (isBlocked) {
            throw new IllegalStateException("User " + name + " is blocked and cannot perform transactions.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Withdrawal amount cannot be negative.");
        }
        if (this.balance - amount < -1e-9) {
            this.isBlocked = true;
            throw new IllegalStateException("Insufficient funds! Account balance would become negative. User " + name + " is now blocked.");
        }
        this.balance -= amount;
        addTransaction(new UserTransaction(reason, -amount, balance));
    }

    /**
     * Adds shares for a specific event option to the user's portfolio.
     */
    public synchronized void addShares(int eventId, String optionName, int count, double cost) {
        holdingsPerEvent.putIfAbsent(eventId, new HashMap<>());
        Map<String, Integer> optionsMap = holdingsPerEvent.get(eventId);
        optionsMap.put(optionName, optionsMap.getOrDefault(optionName, 0) + count);

        investmentPerEvent.putIfAbsent(eventId, new HashMap<>());
        Map<String, Double> costMap = investmentPerEvent.get(eventId);
        costMap.put(optionName, costMap.getOrDefault(optionName, 0.0) + cost);
    }

    public synchronized void addShares(String eventName, String optionName, int count, double cost) {
        String key = eventName.toLowerCase();
        holdingsPerEventName.putIfAbsent(key, new HashMap<>());
        Map<String, Integer> optionsMap = holdingsPerEventName.get(key);
        optionsMap.put(optionName, optionsMap.getOrDefault(optionName, 0) + count);

        investmentPerEventName.putIfAbsent(key, new HashMap<>());
        Map<String, Double> costMap = investmentPerEventName.get(key);
        costMap.put(optionName, costMap.getOrDefault(optionName, 0.0) + cost);
    }

    /**
     * Removes shares for a specific event option from user's portfolio.
     */
    public synchronized void removeShares(int eventId, String optionName, int count) {
        if (!holdingsPerEvent.containsKey(eventId) || !holdingsPerEvent.get(eventId).containsKey(optionName)) {
            throw new IllegalArgumentException("User does not hold shares in option " + optionName);
        }
        Map<String, Integer> optionsMap = holdingsPerEvent.get(eventId);
        int current = optionsMap.getOrDefault(optionName, 0);
        if (current < count) {
            throw new IllegalArgumentException("Cannot remove " + count + " shares, user only holds " + current);
        }
        optionsMap.put(optionName, current - count);
    }

    public synchronized void removeShares(String eventName, String optionName, int count) {
        String key = eventName.toLowerCase();
        if (!holdingsPerEventName.containsKey(key) || !holdingsPerEventName.get(key).containsKey(optionName)) {
            throw new IllegalArgumentException("User does not hold shares in option " + optionName);
        }
        Map<String, Integer> optionsMap = holdingsPerEventName.get(key);
        int current = optionsMap.getOrDefault(optionName, 0);
        if (current < count) {
            throw new IllegalArgumentException("Cannot remove " + count + " shares, user only holds " + current);
        }
        optionsMap.put(optionName, current - count);
    }

    public synchronized int getSharesCount(int eventId, String optionName) {
        if (!holdingsPerEvent.containsKey(eventId)) return 0;
        return holdingsPerEvent.get(eventId).getOrDefault(optionName, 0);
    }

    public synchronized int getSharesCount(String eventName, String optionName) {
        String key = eventName.toLowerCase();
        if (!holdingsPerEventName.containsKey(key)) return 0;
        return holdingsPerEventName.get(key).getOrDefault(optionName, 0);
    }

    public synchronized Map<String, Integer> getHoldingsForEvent(int eventId) {
        return holdingsPerEvent.getOrDefault(eventId, Collections.emptyMap());
    }

    public synchronized Map<String, Integer> getHoldingsForEvent(String eventName) {
        return holdingsPerEventName.getOrDefault(eventName.toLowerCase(), Collections.emptyMap());
    }

    public synchronized Map<Integer, Map<String, Integer>> getAllHoldings() {
        return Collections.unmodifiableMap(holdingsPerEvent);
    }

    public synchronized Map<String, Map<String, Integer>> getAllHoldingsByName() {
        return Collections.unmodifiableMap(holdingsPerEventName);
    }

    public synchronized double getTotalInvestmentForOption(int eventId, String optionName) {
        if (!investmentPerEvent.containsKey(eventId)) return 0.0;
        return investmentPerEvent.get(eventId).getOrDefault(optionName, 0.0);
    }

    public synchronized List<UserTransaction> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }

    private synchronized void addTransaction(UserTransaction transaction) {
        transactionHistory.add(transaction);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return name.equalsIgnoreCase(user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }

    /**
     * Inner class representing a balance history entry.
     */
    public static class UserTransaction implements Serializable {
        private static final long serialVersionUID = 2L;
        private final String description;
        private final double amount;
        private final double balanceAfter;
        private final long timestamp;

        public UserTransaction(String description, double amount, double balanceAfter) {
            this.description = description;
            this.amount = amount;
            this.balanceAfter = balanceAfter;
            this.timestamp = System.currentTimeMillis();
        }

        public String getDescription() { return description; }
        public double getAmount() { return amount; }
        public double getBalanceAfter() { return balanceAfter; }
        public long getTimestamp() { return timestamp; }
    }
}
