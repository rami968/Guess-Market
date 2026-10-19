package engine.dto;

import java.io.Serializable;

// Represents the trading mechanism of an event: LMSR or ORDER_BOOK.
public enum EventType implements Serializable {
    LMSR,
    ORDER_BOOK
}
