package engine.dto;

// Immutable DTO for displaying a transaction record.
public class TransactionDTO {
    private final String optionName;
    private final int shareCount;
    private final double pricePaid;

    public TransactionDTO(String optionName, int shareCount, double pricePaid) {
        this.optionName = optionName;
        this.shareCount = shareCount;
        this.pricePaid = pricePaid;
    }

    public String getOptionName() {
        return optionName;
    }

    public int getShareCount() {
        return shareCount;
    }

    public double getPricePaid() {
        return pricePaid;
    }
}
