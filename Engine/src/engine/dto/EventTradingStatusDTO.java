package engine.dto;

import engine.model.EventStatus;
import java.util.Collections;
import java.util.List;

// Immutable DTO holding full trading status and history of an event.
public class EventTradingStatusDTO {
    private final int eventId;
    private final String eventName;
    private final EventStatus status;
    private final String option1;
    private final String option2;
    private final double prob1;
    private final double prob2;
    private final int q1;
    private final int q2;
    private final double eventAccountBalance;
    private final double totalCommissionCollected;
    private final int winningOptionIndex;
    private final String winningOptionName;
    private final List<TransactionDTO> transactionHistory;

    public EventTradingStatusDTO(int eventId, String eventName, EventStatus status,
                                 String option1, String option2, double prob1, double prob2,
                                 int q1, int q2, double eventAccountBalance, double totalCommissionCollected,
                                 int winningOptionIndex, String winningOptionName,
                                 List<TransactionDTO> transactionHistory) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.status = status;
        this.option1 = option1;
        this.option2 = option2;
        this.prob1 = prob1;
        this.prob2 = prob2;
        this.q1 = q1;
        this.q2 = q2;
        this.eventAccountBalance = eventAccountBalance;
        this.totalCommissionCollected = totalCommissionCollected;
        this.winningOptionIndex = winningOptionIndex;
        this.winningOptionName = winningOptionName;
        this.transactionHistory = Collections.unmodifiableList(transactionHistory);
    }

    public int getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public EventStatus getStatus() {
        return status;
    }

    public String getOption1() {
        return option1;
    }

    public String getOption2() {
        return option2;
    }

    public double getProb1() {
        return prob1;
    }

    public double getProb2() {
        return prob2;
    }

    public int getQ1() {
        return q1;
    }

    public int getQ2() {
        return q2;
    }

    public double getEventAccountBalance() {
        return eventAccountBalance;
    }

    public double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }

    public int getWinningOptionIndex() {
        return winningOptionIndex;
    }

    public String getWinningOptionName() {
        return winningOptionName;
    }

    public List<TransactionDTO> getTransactionHistory() {
        return transactionHistory;
    }
}
