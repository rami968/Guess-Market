package engine.dto;

import java.io.Serializable;

/**
 * Lightweight DTO for displaying other users in the users table.
 */
public class UserSummaryDTO implements Serializable {
    private final String name;
    private final double balance;
    private final boolean isMarketMaker;

    public UserSummaryDTO(String name, double balance, boolean isMarketMaker) {
        this.name = name;
        this.balance = balance;
        this.isMarketMaker = isMarketMaker;
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public boolean isMarketMaker() { return isMarketMaker; }
}
