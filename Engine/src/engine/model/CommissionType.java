package engine.model;

// Represents the commission type of an event.
public enum CommissionType {
    ON_PURCHASE,
    ON_CLOSE;

    // Parses string from XML ("on-purchase" or "on-close").
    public static CommissionType fromString(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Commission type cannot be null");
        }
        String clean = text.trim().toLowerCase();
        if (clean.equals("on-purchase")) {
            return ON_PURCHASE;
        } else if (clean.equals("on-close")) {
            return ON_CLOSE;
        }
        throw new IllegalArgumentException("Unknown commission type: " + text);
    }
}
