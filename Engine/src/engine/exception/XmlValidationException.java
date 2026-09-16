package engine.exception;

// Exception thrown when XML content violates file format or business rules.
public class XmlValidationException extends XMLException {
    public XmlValidationException(String message) {
        super(message);
    }

    public XmlValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
