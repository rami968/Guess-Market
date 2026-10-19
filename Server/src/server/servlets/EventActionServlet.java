package server.servlets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import engine.dto.EventDTO;
import engine.dto.PurchaseResultDTO;
import engine.service.EngineService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.context.EngineContext;
import server.utils.ServletUtils;

import java.io.IOException;

/**
 * Handles all event lifecycle and trading actions:
 * - 'activate': Market Maker activates an inactive event.
 * - 'trade_lmsr': Trader buys shares in an LMSR event.
 * - 'trade_ob': Trader submits a BUY/SELL limit order to an Order Book event.
 * - 'close': Market Maker closes an active event and announces the winning option.
 */
@WebServlet(name = "EventActionServlet", urlPatterns = {"/api/events/action"})
public class EventActionServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getParameter("action");
        String eventName = request.getParameter("eventName");
        String username = request.getParameter("username");

        JsonObject jsonBody = null;
        if (action == null || eventName == null || username == null) {
            String body = ServletUtils.readRequestBody(request);
            if (!body.isEmpty()) {
                try {
                    jsonBody = JsonParser.parseString(body).getAsJsonObject();
                    if (action == null && jsonBody.has("action")) action = jsonBody.get("action").getAsString();
                    if (eventName == null && jsonBody.has("eventName")) eventName = jsonBody.get("eventName").getAsString();
                    if (username == null && jsonBody.has("username")) username = jsonBody.get("username").getAsString();
                } catch (Exception ignored) {
                }
            }
        }

        if (action == null || action.trim().isEmpty()) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Parameter 'action' is required.");
            return;
        }
        if (eventName == null || eventName.trim().isEmpty()) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Parameter 'eventName' is required.");
            return;
        }
        if (username == null || username.trim().isEmpty()) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Parameter 'username' is required.");
            return;
        }

        action = action.trim().toLowerCase();
        eventName = eventName.trim();
        username = username.trim();
        EngineService engine = EngineContext.getEngineService();

        try {
            switch (action) {
                case "activate":
                    engine.activateEventByName(eventName, username);
                    EventDTO activatedEvent = engine.getEventDetailsByName(eventName);
                    ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, activatedEvent);
                    break;

                case "trade_lmsr": {
                    String optIdxStr = getParam(request, jsonBody, "optionIndex");
                    String sharesStr = getParam(request, jsonBody, "sharesCount");
                    int optionIndex = Integer.parseInt(optIdxStr);
                    int sharesCount = Integer.parseInt(sharesStr);

                    PurchaseResultDTO result = engine.buySharesLMSRByName(eventName, username, optionIndex, sharesCount);
                    ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, result);
                    break;
                }

                case "trade_ob": {
                    String optionName = getParam(request, jsonBody, "optionName");
                    String type = getParam(request, jsonBody, "type");
                    String sharesStr = getParam(request, jsonBody, "sharesCount");
                    String priceStr = getParam(request, jsonBody, "price");

                    int sharesCount = Integer.parseInt(sharesStr);
                    double price = Double.parseDouble(priceStr);

                    engine.submitOrderByName(eventName, username, optionName, type, sharesCount, price);
                    EventDTO updatedEvent = engine.getEventDetailsByName(eventName);
                    ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, updatedEvent);
                    break;
                }

                case "close": {
                    String optIdxStr = getParam(request, jsonBody, "winningOptionIndex");
                    int winningOptionIndex = Integer.parseInt(optIdxStr);

                    EventDTO closedEvent = engine.closeEventByName(eventName, username, winningOptionIndex);
                    ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, closedEvent);
                    break;
                }

                default:
                    ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Unknown action: '" + action + "'.");
                    break;
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error executing action: " + e.getMessage());
        }
    }

    private String getParam(HttpServletRequest req, JsonObject json, String name) {
        String val = req.getParameter(name);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        if (json != null && json.has(name) && !json.get(name).isJsonNull()) {
            return json.get(name).getAsString().trim();
        }
        throw new IllegalArgumentException("Missing required parameter: '" + name + "'.");
    }
}
