package server.servlets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import engine.dto.UserDTO;
import engine.service.EngineService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.context.EngineContext;
import server.utils.ServletUtils;

import java.io.IOException;

/**
 * Handles user login and registration.
 * Ensures username uniqueness across the system.
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/api/login"})
public class AuthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        if (username == null || username.trim().isEmpty()) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Username query parameter is required.");
            return;
        }

        EngineService engine = EngineContext.getEngineService();
        try {
            UserDTO user = engine.getUserDetails(username.trim());
            ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, user);
        } catch (IllegalArgumentException e) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "User '" + username.trim() + "' does not exist.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");

        // If not in form parameters, try reading from JSON body
        if (username == null || username.trim().isEmpty()) {
            String body = ServletUtils.readRequestBody(request);
            if (!body.isEmpty()) {
                try {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    if (json.has("username")) {
                        username = json.get("username").getAsString();
                    }
                } catch (Exception ignored) {
                    // Fall back to null
                }
            }
        }

        if (username == null || username.trim().isEmpty()) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Username cannot be empty.");
            return;
        }

        username = username.trim();
        EngineService engine = EngineContext.getEngineService();

        try {
            engine.registerUser(username);
            UserDTO newUser = engine.getUserDetails(username);
            ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, newUser);
        } catch (IllegalArgumentException e) {
            // Already registered or invalid name
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (Exception e) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }
}
