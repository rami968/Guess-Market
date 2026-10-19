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
 * Handles charging / depositing funds into a user's account.
 */
@WebServlet(name = "ChargeServlet", urlPatterns = {"/api/user/charge"})
public class ChargeServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        String amountStr = request.getParameter("amount");

        if (username == null || amountStr == null) {
            String body = ServletUtils.readRequestBody(request);
            if (!body.isEmpty()) {
                try {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    if (json.has("username")) {
                        username = json.get("username").getAsString();
                    }
                    if (json.has("amount")) {
                        amountStr = json.get("amount").getAsString();
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (username == null || username.trim().isEmpty()) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Username parameter is required.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Deposit amount must be greater than 0.");
                return;
            }
        } catch (Exception e) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid deposit amount format.");
            return;
        }

        EngineService engine = EngineContext.getEngineService();
        try {
            engine.depositFunds(username.trim(), amount);
            UserDTO updatedUser = engine.getUserDetails(username.trim());
            ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, updatedUser);
        } catch (IllegalArgumentException e) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }
}
