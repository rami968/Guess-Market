package server.context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import engine.dto.ChatDTO;
import engine.service.EngineService;
import engine.service.EngineServiceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Global context for the Server application.
 * Holds the single EngineService instance, shared Gson serializer,
 * and in-memory chat message history for connected users.
 */
public class EngineContext {

    private static final EngineService engineService = new EngineServiceImpl();
    private static final Gson gson = new GsonBuilder()
            .serializeSpecialFloatingPointValues()
            .create();

    // In-memory chat storage for connected users (Exercise 3 bonus)
    private static final List<ChatDTO> chatMessages = new CopyOnWriteArrayList<>();

    private EngineContext() {
        // Utility / Singleton holder
    }

    public static EngineService getEngineService() {
        return engineService;
    }

    public static Gson getGson() {
        return gson;
    }

    public static void addChatMessage(String username, String message) {
        if (username != null && message != null && !message.trim().isEmpty()) {
            chatMessages.add(new ChatDTO(username.trim(), message.trim(), System.currentTimeMillis()));
        }
    }

    public static List<ChatDTO> getChatMessages(int fromIndex) {
        if (fromIndex < 0 || fromIndex >= chatMessages.size()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(chatMessages.subList(fromIndex, chatMessages.size()));
    }

    public static int getChatMessagesCount() {
        return chatMessages.size();
    }
}
