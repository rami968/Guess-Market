package engine.dto;

import java.io.Serializable;

// Represents the lifecycle status of an event: INACTIVE (not started), ACTIVE (trading), CLOSED (resolved).
public enum EventStatus implements Serializable {
    INACTIVE,
    ACTIVE,
    CLOSED
}
