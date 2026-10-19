package engine.dto;

import java.io.Serializable;

/**
 * DTO for bonus chat room messages.
 */
public class ChatDTO implements Serializable {
    private final String username;
    private final String message;
    private final long timestamp;

    public ChatDTO(String username, String message, long timestamp) {
        this.username = username;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getUsername() { return username; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
}
