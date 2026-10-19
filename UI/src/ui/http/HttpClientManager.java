package ui.http;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import engine.dto.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;

/**
 * Manages all HTTP communication between the JavaFX Client and the Tomcat Server.
 * Uses standard java.net.http.HttpClient and Gson for JSON serialization.
 */
public class HttpClientManager {

    private static HttpClientManager instance;

    private final HttpClient client;
    private final Gson gson;
    private String baseUrl;

    private HttpClientManager() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.gson = new GsonBuilder()
                .serializeSpecialFloatingPointValues()
                .create();
        // Default base URL for Tomcat deployment
        this.baseUrl = "http://localhost:8080/guess_market";
    }

    public static synchronized HttpClientManager getInstance() {
        if (instance == null) {
            instance = new HttpClientManager();
        }
        return instance;
    }

    public void setBaseUrl(String baseUrl) {
        if (baseUrl != null && baseUrl.endsWith("/")) {
            this.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            this.baseUrl = baseUrl;
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Checks server reachability and automatically adjusts context path if needed.
     */
    public boolean pingServer() {
        String[] possibleUrls = {
                baseUrl,
                "http://localhost:8080/guess_market",
                "http://localhost:8080/Server_war",
                "http://localhost:8080/Server_war_exploded",
                "http://localhost:8080"
        };

        for (String url : possibleUrls) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url + "/ping"))
                        .timeout(Duration.ofSeconds(2))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    this.baseUrl = url;
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    // ==================== User & Authentication ====================

    public UserDTO login(String username) throws IOException, InterruptedException {
        String jsonBody = gson.toJson(Map.of("username", username));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/login"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), UserDTO.class);
        }
        throw new IOException(extractErrorMessage(response.body(), "Login failed (HTTP " + response.statusCode() + ")"));
    }

    public UserDTO getUserDetails(String username) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(username, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/users?username=" + encoded))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), UserDTO.class);
        }
        throw new IOException(extractErrorMessage(response.body(), "Failed to get user details"));
    }

    public List<UserSummaryDTO> getAllUserSummaries() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/users"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            Type listType = new TypeToken<List<UserSummaryDTO>>() {}.getType();
            return gson.fromJson(response.body(), listType);
        }
        return Collections.emptyList();
    }

    public UserDTO chargeBalance(String username, double amount) throws IOException, InterruptedException {
        String jsonBody = gson.toJson(Map.of("username", username, "amount", String.valueOf(amount)));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/user/charge"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), UserDTO.class);
        }
        throw new IOException(extractErrorMessage(response.body(), "Deposit failed"));
    }

    // ==================== XML Upload ====================

    public String uploadXml(File xmlFile, String username) throws IOException, InterruptedException {
        String boundary = "---GuessMarketBoundary" + System.currentTimeMillis();
        byte[] fileBytes = Files.readAllBytes(xmlFile.toPath());
        String fileName = xmlFile.getName();

        StringBuilder header = new StringBuilder();
        // Username part
        header.append("--").append(boundary).append("\r\n");
        header.append("Content-Disposition: form-data; name=\"username\"\r\n\r\n");
        header.append(username).append("\r\n");

        // File part
        header.append("--").append(boundary).append("\r\n");
        header.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
        header.append("Content-Type: text/xml\r\n\r\n");

        byte[] headerBytes = header.toString().getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        byte[] fullPayload = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, fullPayload, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, fullPayload, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, fullPayload, headerBytes.length + fileBytes.length, footerBytes.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/events/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofByteArray(fullPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            return json.has("message") ? json.get("message").getAsString() : "Events loaded successfully.";
        }
        throw new IOException(extractErrorMessage(response.body(), "Failed to upload XML file"));
    }

    // ==================== Events & Trading ====================

    public List<EventDTO> getAllEvents() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/events"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            Type listType = new TypeToken<List<EventDTO>>() {}.getType();
            return gson.fromJson(response.body(), listType);
        }
        return Collections.emptyList();
    }

    public EventTradingStatusDTO getEventTradingStatus(String eventName) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(eventName, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/events?name=" + encoded + "&trading=true"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), EventTradingStatusDTO.class);
        }
        throw new IOException(extractErrorMessage(response.body(), "Failed to fetch event trading status"));
    }

    public EventDTO activateEvent(String eventName, String username) throws IOException, InterruptedException {
        Map<String, String> payload = Map.of(
                "action", "activate",
                "eventName", eventName,
                "username", username
        );
        return sendActionRequest(payload, EventDTO.class);
    }

    public PurchaseResultDTO tradeLMSR(String eventName, String username, int optionIndex, int sharesCount) throws IOException, InterruptedException {
        Map<String, String> payload = Map.of(
                "action", "trade_lmsr",
                "eventName", eventName,
                "username", username,
                "optionIndex", String.valueOf(optionIndex),
                "sharesCount", String.valueOf(sharesCount)
        );
        return sendActionRequest(payload, PurchaseResultDTO.class);
    }

    public EventDTO tradeOrderBook(String eventName, String username, String optionName, String type, int sharesCount, double price) throws IOException, InterruptedException {
        Map<String, String> payload = Map.of(
                "action", "trade_ob",
                "eventName", eventName,
                "username", username,
                "optionName", optionName,
                "type", type,
                "sharesCount", String.valueOf(sharesCount),
                "price", String.valueOf(price)
        );
        return sendActionRequest(payload, EventDTO.class);
    }

    public EventDTO closeEvent(String eventName, String username, int winningOptionIndex) throws IOException, InterruptedException {
        Map<String, String> payload = Map.of(
                "action", "close",
                "eventName", eventName,
                "username", username,
                "winningOptionIndex", String.valueOf(winningOptionIndex)
        );
        return sendActionRequest(payload, EventDTO.class);
    }

    private <T> T sendActionRequest(Map<String, String> payload, Class<T> responseClass) throws IOException, InterruptedException {
        String jsonBody = gson.toJson(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/events/action"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), responseClass);
        }
        throw new IOException(extractErrorMessage(response.body(), "Action failed (HTTP " + response.statusCode() + ")"));
    }

    // ==================== Sync & Polling ====================

    public static class SyncResult {
        public int version;
        public int chatCount;
        public long timestamp;
    }

    public SyncResult checkSync() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/sync"))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), SyncResult.class);
        }
        return null;
    }

    // ==================== Chat (Bonus) ====================

    public static class ChatResponse {
        public List<ChatDTO> messages;
        public int nextIndex;
    }

    public ChatResponse getChatMessages(int fromIndex) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat?from=" + fromIndex))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), ChatResponse.class);
        }
        return null;
    }

    public boolean sendChatMessage(String username, String message) throws IOException, InterruptedException {
        String jsonBody = gson.toJson(Map.of("username", username, "message", message));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(3))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200;
    }

    // ==================== Utilities ====================

    private String extractErrorMessage(String responseBody, String defaultMsg) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return defaultMsg;
        }
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("error")) {
                return json.get("error").getAsString();
            }
        } catch (Exception ignored) {
        }
        return responseBody;
    }
}
