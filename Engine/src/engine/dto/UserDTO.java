package engine.dto;

import java.io.Serializable;
import java.util.*;

/**
 * Data Transfer Object representing user information for the UI layer.
 */
public class UserDTO implements Serializable {
    private final String name;
    private final double balance;
    private final boolean isBlocked;
    private final boolean isMarketMaker;
    private final Set<Integer> marketMakerEventIds;
    private final Set<String> marketMakerEventNames;
    private final Map<Integer, Map<String, Integer>> holdingsPerEvent;
    private final Map<String, Map<String, Integer>> holdingsPerEventName;
    private final List<UserTransactionDTO> transactions;

    public UserDTO(String name, double balance, boolean isBlocked, boolean isMarketMaker,
                   Set<Integer> marketMakerEventIds, Set<String> marketMakerEventNames,
                   Map<Integer, Map<String, Integer>> holdingsPerEvent,
                   Map<String, Map<String, Integer>> holdingsPerEventName,
                   List<UserTransactionDTO> transactions) {
        this.name = name;
        this.balance = balance;
        this.isBlocked = isBlocked;
        this.isMarketMaker = isMarketMaker;
        this.marketMakerEventIds = (marketMakerEventIds != null) ? Collections.unmodifiableSet(new HashSet<>(marketMakerEventIds)) : Collections.emptySet();
        this.marketMakerEventNames = (marketMakerEventNames != null) ? Collections.unmodifiableSet(new HashSet<>(marketMakerEventNames)) : Collections.emptySet();
        this.holdingsPerEvent = (holdingsPerEvent != null) ? Collections.unmodifiableMap(holdingsPerEvent) : Collections.emptyMap();
        this.holdingsPerEventName = (holdingsPerEventName != null) ? Collections.unmodifiableMap(holdingsPerEventName) : Collections.emptyMap();
        this.transactions = (transactions != null) ? Collections.unmodifiableList(new ArrayList<>(transactions)) : Collections.emptyList();
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public boolean isBlocked() { return isBlocked; }
    public boolean isMarketMaker() { return isMarketMaker; }
    public Set<Integer> getMarketMakerEventIds() { return marketMakerEventIds; }
    public Set<String> getMarketMakerEventNames() { return marketMakerEventNames; }
    public Map<Integer, Map<String, Integer>> getHoldingsPerEvent() { return holdingsPerEvent; }
    public Map<String, Map<String, Integer>> getHoldingsPerEventName() { return holdingsPerEventName; }
    public List<UserTransactionDTO> getTransactions() { return transactions; }

    public static class UserTransactionDTO implements Serializable {
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
