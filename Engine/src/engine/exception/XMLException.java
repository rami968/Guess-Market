package engine.exception;

// Base unchecked exception for XML loading and processing errors.
public class XMLException extends RuntimeException {
    public XMLException(String message) {
        super(message);
    }

    public XMLException(String message, Throwable cause) {
        super(message, cause);
    }
}
