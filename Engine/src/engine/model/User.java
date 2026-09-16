package engine.model;

import java.util.*;

/**
 * Represents a user in the Guess Market application.
 * Users can be regular traders or Market Makers (MM) for specific events.
 * Keeps track of account balance, active holdings per event option, and transactions.
 */
public class User {
    private final String name;
    private double balance;
    private boolean isBlocked;
    private final Set<Integer> marketMakerEventIds;
    
    // Tracks share holdings per event and option: Map<EventID, Map<OptionName, ShareCount>>
    private final Map<Integer, Map<String, Integer>> holdingsPerEvent;
    
    // Tracks total amount paid per event option for profit/loss calculation: Map<EventID, Map<OptionName, TotalSpent>>
    private final Map<Integer, Map<String, Double>> investmentPerEvent;

    // List of account balance transactions history
    private final List<UserTransaction> transactionHistory;

    public User(String name, double initialCash) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be null or empty");
        }
        if (initialCash <= 0) {
            throw new IllegalArgumentException("Initial balance for user must be greater than 0");
        }
        this.name = name.trim();
        this.balance = initialCash;
        this.isBlocked = false;
        this.marketMakerEventIds = new HashSet<>();
        this.holdingsPerEvent = new HashMap<>();
        this.investmentPerEvent = new HashMap<>();
        this.transactionHistory = new ArrayList<>();

        // Record initial deposit transaction
        addTransaction(new UserTransaction("Initial Deposit", initialCash, balance));
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        this.isBlocked = blocked;
    }

    public Set<Integer> getMarketMakerEventIds() {
        return Collections.unmodifiableSet(marketMakerEventIds);
    }

    public void addMarketMakerEventId(int eventId) {
        marketMakerEventIds.add(eventId);
    }

    public boolean isMarketMakerFor(int eventId) {
        return marketMakerEventIds.contains(eventId);
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
     * If the withdrawal would result in a negative balance, blocks the user and throws IllegalStateException.
     */
    public synchronized void withdraw(double amount, String reason) {
        if (isBlocked) {
            throw new IllegalStateException("User " + name + " is blocked and cannot perform transactions.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Withdrawal amount cannot be negative.");
        }
        if (this.balance - amount < 0) {
            this.isBlocked = true;
            throw new IllegalStateException("Insufficient funds! Account balance would become negative. User " + name + " is now blocked.");
        }
        this.balance -= amount;
        addTransaction(new UserTransaction(reason, -amount, balance));
    }

    /**
     * Adds shares for a specific event option to the user's portfolio.
     */
    public void addShares(int eventId, String optionName, int count, double cost) {
        holdingsPerEvent.putIfAbsent(eventId, new HashMap<>());
        Map<String, Integer> optionsMap = holdingsPerEvent.get(eventId);
        optionsMap.put(optionName, optionsMap.getOrDefault(optionName, 0) + count);

        investmentPerEvent.putIfAbsent(eventId, new HashMap<>());
        Map<String, Double> costMap = investmentPerEvent.get(eventId);
        costMap.put(optionName, costMap.getOrDefault(optionName, 0.0) + cost);
    }

    /**
     * Removes shares for a specific event option from user's portfolio.
     */
    public void removeShares(int eventId, String optionName, int count) {
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

    public int getSharesCount(int eventId, String optionName) {
        if (!holdingsPerEvent.containsKey(eventId)) return 0;
        return holdingsPerEvent.get(eventId).getOrDefault(optionName, 0);
    }

    public Map<String, Integer> getHoldingsForEvent(int eventId) {
        return holdingsPerEvent.getOrDefault(eventId, Collections.emptyMap());
    }

    public Map<Integer, Map<String, Integer>> getAllHoldings() {
        return Collections.unmodifiableMap(holdingsPerEvent);
    }

    public double getTotalInvestmentForOption(int eventId, String optionName) {
        if (!investmentPerEvent.containsKey(eventId)) return 0.0;
        return investmentPerEvent.get(eventId).getOrDefault(optionName, 0.0);
    }

    public List<UserTransaction> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }

    private void addTransaction(UserTransaction transaction) {
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
    public static class UserTransaction {
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
