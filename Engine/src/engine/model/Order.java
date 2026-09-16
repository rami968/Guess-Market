package engine.model;

import java.util.Objects;

/**
 * Represents a single trading order submitted by a user in an Order Book event.
 */
public class Order {
    private static int idCounter = 1;

    private final int id;
    private final String userName;
    private final String optionName;
    private final OrderType type;
    private int sharesCount;
    private final double pricePerShare;
    private final long timestamp;

    public Order(String userName, String optionName, OrderType type, int sharesCount, double pricePerShare, double baseValue) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be empty");
        }
        if (optionName == null || optionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Option name cannot be empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Order type cannot be null");
        }
        if (sharesCount <= 0) {
            throw new IllegalArgumentException("Shares count must be greater than 0");
        }
        double maxPrice = baseValue - 0.01;
        if (pricePerShare <= 0 || pricePerShare > maxPrice + 1e-9) {
            throw new IllegalArgumentException(String.format("Price per share must be positive and cannot exceed %.2f (base value %.2f - 0.01)", maxPrice, baseValue));
        }

        this.id = idCounter++;
        this.userName = userName.trim();
        this.optionName = optionName.trim();
        this.type = type;
        this.sharesCount = sharesCount;
        this.pricePerShare = pricePerShare;
        this.timestamp = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public String getUserName() { return userName; }
    public String getOptionName() { return optionName; }
    public OrderType getType() { return type; }
    public int getSharesCount() { return sharesCount; }
    public void setSharesCount(int sharesCount) { this.sharesCount = sharesCount; }
    public double getPricePerShare() { return pricePerShare; }
    public long getTimestamp() { return timestamp; }

    /**
     * Reduces the number of remaining shares in this order by count.
     */
    public void consumeShares(int count) {
        if (count > sharesCount) {
            throw new IllegalArgumentException("Cannot consume more shares than available in order");
        }
        this.sharesCount -= count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return id == order.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
