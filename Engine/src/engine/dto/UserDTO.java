package engine.dto;

import java.util.*;

/**
 * Data Transfer Object representing user information for the UI layer.
 */
public class UserDTO {
    private final String name;
    private final double balance;
    private final boolean isBlocked;
    private final Set<Integer> marketMakerEventIds;
    private final Map<Integer, Map<String, Integer>> holdingsPerEvent;
    private final List<UserTransactionDTO> transactions;

    public UserDTO(String name, double balance, boolean isBlocked, Set<Integer> marketMakerEventIds,
                   Map<Integer, Map<String, Integer>> holdingsPerEvent, List<UserTransactionDTO> transactions) {
        this.name = name;
        this.balance = balance;
        this.isBlocked = isBlocked;
        this.marketMakerEventIds = Collections.unmodifiableSet(new HashSet<>(marketMakerEventIds));
        this.holdingsPerEvent = Collections.unmodifiableMap(holdingsPerEvent);
        this.transactions = Collections.unmodifiableList(new ArrayList<>(transactions));
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public boolean isBlocked() { return isBlocked; }
    public Set<Integer> getMarketMakerEventIds() { return marketMakerEventIds; }
    public Map<Integer, Map<String, Integer>> getHoldingsPerEvent() { return holdingsPerEvent; }
    public List<UserTransactionDTO> getTransactions() { return transactions; }

    public static class UserTransactionDTO {
        private final String description;
        private final double amount;
        private final double balanceAfter;
        private final long timestamp;

        public UserTransactionDTO(String description, double amount, double balanceAfter, long timestamp) {
            this.description = description;
            this.amount = amount;
            this.balanceAfter = balanceAfter;
            this.timestamp = timestamp;
        }

        public String getDescription() { return description; }
        public double getAmount() { return amount; }
        public double getBalanceAfter() { return balanceAfter; }
        public long getTimestamp() { return timestamp; }
    }
}
