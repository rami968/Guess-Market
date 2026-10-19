package server.servlets;

import com.google.gson.JsonObject;
import engine.exception.XmlValidationException;
import engine.service.EngineService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import server.context.EngineContext;
import server.utils.ServletUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Handles uploading of XML files containing events.
 * The uploading user becomes the Market Maker for all loaded events.
 * XML is parsed in-memory without saving files to the server's disk.
 */
@WebServlet(name = "UploadEventServlet", urlPatterns = {"/api/events/upload"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1 MB
        maxFileSize = 10 * 1024 * 1024,        // 10 MB
        maxRequestSize = 20 * 1024 * 1024      // 20 MB
)
public class UploadEventServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        InputStream xmlStream = null;

        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
            try {
                Part filePart = request.getPart("file");
                if (filePart != null) {
                    xmlStream = filePart.getInputStream();
                }
                if (username == null || username.trim().isEmpty()) {
                    Part userPart = request.getPart("username");
                    if (userPart != null) {
                        username = new String(userPart.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                    }
                }
            } catch (Exception e) {
                ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Failed to read multipart data: " + e.getMessage());
                return;
            }
        } else {
            // Direct XML payload
            String rawBody = ServletUtils.readRequestBody(request);
            if (!rawBody.isEmpty()) {
                xmlStream = new ByteArrayInputStream(rawBody.getBytes(StandardCharsets.UTF_8));
            }
        }

        if (username == null || username.trim().isEmpty()) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Username of the Market Maker is required.");
            return;
        }

        if (xmlStream == null) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "No XML file or content provided.");
            return;
        }

        EngineService engine = EngineContext.getEngineService();
        try {
            int loadedCount = engine.loadXmlStream(xmlStream, username.trim());
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("loadedCount", loadedCount);
            result.addProperty("message", "Successfully loaded " + loadedCount + " event(s) for Market Maker '" + username.trim() + "'.");
            ServletUtils.sendJsonResponse(response, HttpServletResponse.SC_OK, result);
        } catch (XmlValidationException e) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "XML Validation Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error parsing XML: " + e.getMessage());
        }
    }
}
