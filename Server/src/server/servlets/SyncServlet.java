package server.servlets;

import com.google.gson.JsonObject;
import engine.service.EngineService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.context.EngineContext;
import server.utils.ServletUtils;

import java.io.IOException;

/**
 * Lightweight synchronization servlet for polling.
 * Returns the current state version counter and server timestamp.
 */
@WebServlet(name = "SyncServlet", urlPatterns = {"/api/sync"})
public class SyncServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EngineService engine = EngineContext.getEngineService();
        JsonObject syncData = new JsonObject();
        syncData.addProperty("version", engine.getVersion());
        syncData.addProperty("chatCount", EngineContext.getChatMessagesCount());
        syncData.addProperty("timestamp", System.currentTimeMillis());

        ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, syncData);
    }
}
