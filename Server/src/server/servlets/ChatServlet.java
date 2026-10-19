package server.servlets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import engine.dto.ChatDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.context.EngineContext;
import server.utils.ServletUtils;

import java.io.IOException;
import java.util.List;

/**
 * Handles multi-user chat room functionality (Bonus - 5 points).
 * Enables all connected clients to send and receive real-time messages.
 */
@WebServlet(name = "ChatServlet", urlPatterns = {"/api/chat"})
public class ChatServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String fromStr = request.getParameter("from");
        int fromIndex = 0;
        if (fromStr != null) {
            try {
                fromIndex = Integer.parseInt(fromStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        List<ChatDTO> messages = EngineContext.getChatMessages(fromIndex);
        JsonObject result = new JsonObject();
        result.add("messages", EngineContext.getGson().toJsonTree(messages));
        result.addProperty("nextIndex", EngineContext.getChatMessagesCount());

        ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, result);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        String message = request.getParameter("message");

        if (username == null || message == null) {
            String body = ServletUtils.readRequestBody(request);
            if (!body.isEmpty()) {
                try {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    if (username == null && json.has("username")) username = json.get("username").getAsString();
                    if (message == null && json.has("message")) message = json.get("message").getAsString();
                } catch (Exception ignored) {
                }
            }
        }

        if (username == null || username.trim().isEmpty()) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Username is required.");
            return;
        }
        if (message == null || message.trim().isEmpty()) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Message text cannot be empty.");
            return;
        }

        EngineContext.addChatMessage(username.trim(), message.trim());

        JsonObject responseObj = new JsonObject();
        responseObj.addProperty("success", true);
        responseObj.addProperty("nextIndex", EngineContext.getChatMessagesCount());
        ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, responseObj);
    }
}
