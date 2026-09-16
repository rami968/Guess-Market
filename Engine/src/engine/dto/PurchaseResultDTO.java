package engine.dto;

// Immutable DTO for purchase receipt returned to UI after buying shares.
public class PurchaseResultDTO {
    private final double sharePrice;
    private final double commissionPaid;
    private final double totalPaid;
    private final EventTradingStatusDTO updatedStatus;

    public PurchaseResultDTO(double sharePrice, double commissionPaid, EventTradingStatusDTO updatedStatus) {
        this.sharePrice = sharePrice;
        this.commissionPaid = commissionPaid;
        this.totalPaid = sharePrice + commissionPaid;
        this.updatedStatus = updatedStatus;
    }

    public double getSharePrice() {
        return sharePrice;
    }

    public double getCommissionPaid() {
        return commissionPaid;
    }

    public double getTotalPaid() {
        return totalPaid;
    }

    public EventTradingStatusDTO getUpdatedStatus() {
        return updatedStatus;
    }
}
