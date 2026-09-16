package engine.model;

/**
 * Represents an executed trade in the Order Book (matching buyer and seller).
 */
public class OrderBookTrade {
    private final String buyerName;
    private final String sellerName;
    private final String optionName;
    private final int sharesCount;
    private final double pricePerShare;
    private final double totalAmount;
    private final double commissionPaid;
    private final long timestamp;

    public OrderBookTrade(String buyerName, String sellerName, String optionName, int sharesCount, double pricePerShare, double totalAmount, double commissionPaid) {
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.optionName = optionName;
        this.sharesCount = sharesCount;
        this.pricePerShare = pricePerShare;
        this.totalAmount = totalAmount;
        this.commissionPaid = commissionPaid;
        this.timestamp = System.currentTimeMillis();
    }

    public String getBuyerName() { return buyerName; }
    public String getSellerName() { return sellerName; }
    public String getOptionName() { return optionName; }
    public int getSharesCount() { return sharesCount; }
    public double getPricePerShare() { return pricePerShare; }
    public double getTotalAmount() { return totalAmount; }
    public double getCommissionPaid() { return commissionPaid; }
    public long getTimestamp() { return timestamp; }
}
