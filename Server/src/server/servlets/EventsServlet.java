package server.servlets;

import engine.dto.EventDTO;
import engine.dto.EventTradingStatusDTO;
import engine.service.EngineService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.context.EngineContext;
import server.utils.ServletUtils;

import java.io.IOException;
import java.util.List;

/**
 * Handles fetching event information:
 * - List of all events (or filtered by active).
 * - Specific event details by name or id.
 * - Trading status and Order Book state for a specific event.
 */
@WebServlet(name = "EventsServlet", urlPatterns = {"/api/events"})
public class EventsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String eventName = request.getParameter("name");
        String eventIdStr = request.getParameter("id");
        String isTrading = request.getParameter("trading");
        String statusFilter = request.getParameter("status");

        EngineService engine = EngineContext.getEngineService();

        try {
            if (eventName != null && !eventName.trim().isEmpty()) {
                if ("true".equalsIgnoreCase(isTrading)) {
                    EventTradingStatusDTO status = engine.getEventTradingStatusByName(eventName.trim());
                    ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, status);
                } else {
                    EventDTO event = engine.getEventDetailsByName(eventName.trim());
                    ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, event);
                }
                return;
            }

            if (eventIdStr != null && !eventIdStr.trim().isEmpty()) {
                int eventId = Integer.parseInt(eventIdStr.trim());
                if ("true".equalsIgnoreCase(isTrading)) {
                    EventTradingStatusDTO status = engine.getEventTradingStatus(eventId);
                    ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, status);
                } else {
                    EventDTO event = engine.getEventDetails(eventId);
                    ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, event);
                }
                return;
            }

            // Return all events or active events
            List<EventDTO> events;
            if ("active".equalsIgnoreCase(statusFilter)) {
                events = engine.getActiveEvents();
            } else {
                events = engine.getAllEvents();
            }
            ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, events);

        } catch (IllegalArgumentException e) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }
}
