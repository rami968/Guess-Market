package engine.dto;

/**
 * Data Transfer Object representing an order in the Order Book for UI tables.
 */
public class OrderDTO {
    private final int id;
    private final String userName;
    private final String optionName;
    private final String type; // "BUY" or "SELL"
    private final int sharesCount;
    private final double pricePerShare;
    private final long timestamp;

    public OrderDTO(int id, String userName, String optionName, String type, int sharesCount, double pricePerShare, long timestamp) {
        this.id = id;
        this.userName = userName;
        this.optionName = optionName;
        this.type = type;
        this.sharesCount = sharesCount;
        this.pricePerShare = pricePerShare;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public String getUserName() { return userName; }
    public String getOptionName() { return optionName; }
    public String getType() { return type; }
    public int getSharesCount() { return sharesCount; }
    public double getPricePerShare() { return pricePerShare; }
    public long getTimestamp() { return timestamp; }
}
