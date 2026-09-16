package engine.dto;

import engine.model.CommissionType;
import engine.model.EventStatus;
import engine.model.EventType;

/**
 * Immutable DTO representing comprehensive event information for the UI.
 */
public class EventDTO {
    private final int id;
    private final String name;
    private final String description;
    private final int commission;
    private final CommissionType commissionType;
    private final String option1;
    private final String option2;
    private final EventType eventType;
    private final String marketMakerName;
    private final EventStatus status;
    private final double accountBalance;
    private final double totalCommissionCollected;
    private final double probabilityOption1;
    private final double probabilityOption2;

    private final OrderBookStatsDTO orderBookStatsOption1;
    private final OrderBookStatsDTO orderBookStatsOption2;

    public EventDTO(int id, String name, String description, int commission,
                    CommissionType commissionType, String option1, String option2,
                    EventType eventType, String marketMakerName, EventStatus status,
                    double accountBalance, double totalCommissionCollected,
                    double probabilityOption1, double probabilityOption2,
                    OrderBookStatsDTO orderBookStatsOption1, OrderBookStatsDTO orderBookStatsOption2) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.option1 = option1;
        this.option2 = option2;
        this.eventType = eventType;
        this.marketMakerName = marketMakerName;
        this.status = status;
        this.accountBalance = accountBalance;
        this.totalCommissionCollected = totalCommissionCollected;
        this.probabilityOption1 = probabilityOption1;
        this.probabilityOption2 = probabilityOption2;
        this.orderBookStatsOption1 = orderBookStatsOption1;
        this.orderBookStatsOption2 = orderBookStatsOption2;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCommission() { return commission; }
    public CommissionType getCommissionType() { return commissionType; }
    public String getOption1() { return option1; }
    public String getOption2() { return option2; }
    public EventType getEventType() { return eventType; }
    public String getMarketMakerName() { return marketMakerName; }
    public EventStatus getStatus() { return status; }
    public double getAccountBalance() { return accountBalance; }
    public double getTotalCommissionCollected() { return totalCommissionCollected; }
    public double getProbabilityOption1() { return probabilityOption1; }
    public double getProbabilityOption2() { return probabilityOption2; }

    public OrderBookStatsDTO getOrderBookStatsOption1() { return orderBookStatsOption1; }
    public OrderBookStatsDTO getOrderBookStatsOption2() { return orderBookStatsOption2; }
}
