package server.utils;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.context.EngineContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Utility helper methods for handling HTTP servlet requests and responses.
 */
public class ServletUtils {

    private ServletUtils() {
        // Static utility methods
    }

    /**
     * Sends a JSON response with the given HTTP status code and serializable object.
     */
    public static void sendJsonResponse(HttpServletResponse response, int statusCode, Object data) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(statusCode);
        try (PrintWriter out = response.getWriter()) {
            out.print(EngineContext.getGson().toJson(data));
        }
    }

    /**
     * Sends an error response formatted as JSON { "error": "message" }.
     */
    public static void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(statusCode);
        JsonObject errorObj = new JsonObject();
        errorObj.addProperty("error", message);
        try (PrintWriter out = response.getWriter()) {
            out.print(EngineContext.getGson().toJson(errorObj));
        }
    }

    /**
     * Reads the entire body of an HTTP request as a string.
     */
    public static String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Retrieves a query or form parameter, throwing IllegalArgumentException if missing or empty.
     */
    public static String getRequiredParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required parameter: '" + name + "'.");
        }
        return value.trim();
    }

    /**
     * Retrieves a parameter with a fallback default value if null or empty.
     */
    public static String getParameter(HttpServletRequest request, String name, String defaultValue) {
        String value = request.getParameter(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }
}
