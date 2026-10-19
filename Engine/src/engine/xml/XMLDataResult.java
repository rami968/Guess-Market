package engine.xml;

import engine.model.GMEvent;
import engine.model.User;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Container holding the result of parsing an XML file: loaded events and optional users.
 */
public class XMLDataResult {
    private final List<GMEvent> events;
    private final Map<String, User> users;

    public XMLDataResult(List<GMEvent> events) {
        this(events, Collections.emptyMap());
    }

    public XMLDataResult(List<GMEvent> events, Map<String, User> users) {
        this.events = events;
        this.users = (users != null) ? users : Collections.emptyMap();
    }

    public List<GMEvent> getEvents() { return events; }
    public Map<String, User> getUsers() { return users; }
}
