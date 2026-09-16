package engine.model;

import java.io.Serializable;

// Stores information for a single buy transaction.
public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String optionName;
    private final int shareCount;
    private final double pricePaid;

    public Transaction(String optionName, int shareCount, double pricePaid) {
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
