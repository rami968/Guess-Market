package engine.model;

/**
 * Enum defining the type of an order in the Order Book trading mechanism.
 * BUY = Bid (willing to purchase shares at a maximum price)
 * SELL = Ask (willing to sell shares at a minimum price)
 */
public enum OrderType {
    BUY,
    SELL
}
