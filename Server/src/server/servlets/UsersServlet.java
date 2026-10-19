package server.servlets;

import engine.dto.UserDTO;
import engine.dto.UserSummaryDTO;
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
 * Handles fetching user data.
 * - If 'username' parameter is passed: returns full UserDTO (balance, holdings, transactions).
 * - Otherwise: returns List<UserSummaryDTO> for the other users overview table.
 */
@WebServlet(name = "UsersServlet", urlPatterns = {"/api/users"})
public class UsersServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        EngineService engine = EngineContext.getEngineService();

        try {
            if (username != null && !username.trim().isEmpty()) {
                UserDTO user = engine.getUserDetails(username.trim());
                ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, user);
            } else {
                List<UserSummaryDTO> summaries = engine.getAllUserSummaries();
                ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, summaries);
            }
        } catch (IllegalArgumentException e) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }
}
