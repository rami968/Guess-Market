package engine.dto;

import java.io.Serializable;

// Represents the order type in Order Book: BUY or SELL.
public enum OrderType implements Serializable {
    BUY,
    SELL
}
