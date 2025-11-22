package io.github.headlesshq.headlessmc.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.CustomLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@CustomLog
public class YggdrasilClient {
    private final String authServerUrl;

    public YggdrasilClient(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public YggdrasilSession authenticate(String username, String password) throws IOException {
        log.debug("Authenticating with Yggdrasil server: " + authServerUrl);
        
        // Ensure URL format is correct: if authServerUrl ends with /, remove it
        String baseUrl = authServerUrl.endsWith("/") ? authServerUrl.substring(0, authServerUrl.length() - 1) : authServerUrl;
        URL url = new URL(baseUrl + "/authserver/authenticate");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Build request payload
        JsonObject request = new JsonObject();
        JsonObject agent = new JsonObject();
        agent.addProperty("name", "Minecraft");
        agent.addProperty("version", 1);
        request.add("agent", agent);
        request.addProperty("username", username);
        request.addProperty("password", password);
        request.addProperty("requestUser", true);

        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            os.write(request.toString().getBytes(StandardCharsets.UTF_8));
        }

        // Read response
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String errorMessage = readErrorResponse(conn);
            throw new IOException("Authentication failed: " + responseCode + " - " + errorMessage);
        }

        String response = readResponse(conn);
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();

        // Parse response
        if (!jsonResponse.has("accessToken")) {
            throw new IOException("Invalid response from authentication server: missing accessToken");
        }

        String accessToken = jsonResponse.get("accessToken").getAsString();
        String clientToken = jsonResponse.has("clientToken") 
            ? jsonResponse.get("clientToken").getAsString() 
            : null;

        // Handle multi-profile situation
        List<YggdrasilSession.Profile> availableProfiles = new ArrayList<>();
        YggdrasilSession.Profile selectedProfile = null;

        // Parse availableProfiles (all available profiles)
        if (jsonResponse.has("availableProfiles") && jsonResponse.get("availableProfiles").isJsonArray()) {
            com.google.gson.JsonArray profilesArray = jsonResponse.getAsJsonArray("availableProfiles");
            for (com.google.gson.JsonElement element : profilesArray) {
                JsonObject profileObj = element.getAsJsonObject();
                availableProfiles.add(new YggdrasilSession.Profile(
                    profileObj.get("id").getAsString(),
                    profileObj.get("name").getAsString()
                ));
            }
        }

        // Parse selectedProfile (currently selected profile)
        if (jsonResponse.has("selectedProfile") && !jsonResponse.get("selectedProfile").isJsonNull()) {
            JsonObject selectedProfileObj = jsonResponse.getAsJsonObject("selectedProfile");
            selectedProfile = new YggdrasilSession.Profile(
                selectedProfileObj.get("id").getAsString(),
                selectedProfileObj.get("name").getAsString()
            );
        } else if (!availableProfiles.isEmpty()) {
            // If no selectedProfile but has availableProfiles, use the first one
            selectedProfile = availableProfiles.get(0);
        } else {
            throw new IOException("No profile available in authentication response");
        }

        return new YggdrasilSession(accessToken, clientToken, 
            selectedProfile.getId(), selectedProfile.getName(),
            availableProfiles, selectedProfile);
    }

    public YggdrasilSession refresh(String accessToken, String clientToken) throws IOException {
        log.debug("Refreshing Yggdrasil session");
        
        String baseUrl = authServerUrl.endsWith("/") ? authServerUrl.substring(0, authServerUrl.length() - 1) : authServerUrl;
        URL url = new URL(baseUrl + "/authserver/refresh");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JsonObject request = new JsonObject();
        request.addProperty("accessToken", accessToken);
        if (clientToken != null) {
            request.addProperty("clientToken", clientToken);
        }
        request.addProperty("requestUser", true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(request.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String errorMessage = readErrorResponse(conn);
            throw new IOException("Refresh failed: " + responseCode + " - " + errorMessage);
        }

        String response = readResponse(conn);
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();

        if (!jsonResponse.has("accessToken")) {
            throw new IOException("Invalid response from refresh server");
        }

        String newAccessToken = jsonResponse.get("accessToken").getAsString();
        String newClientToken = jsonResponse.has("clientToken") 
            ? jsonResponse.get("clientToken").getAsString() 
            : clientToken;
        
        List<YggdrasilSession.Profile> availableProfiles = new ArrayList<>();
        YggdrasilSession.Profile selectedProfile = null;

        if (jsonResponse.has("availableProfiles") && jsonResponse.get("availableProfiles").isJsonArray()) {
            com.google.gson.JsonArray profilesArray = jsonResponse.getAsJsonArray("availableProfiles");
            for (com.google.gson.JsonElement element : profilesArray) {
                JsonObject profileObj = element.getAsJsonObject();
                availableProfiles.add(new YggdrasilSession.Profile(
                    profileObj.get("id").getAsString(),
                    profileObj.get("name").getAsString()
                ));
            }
        }

        if (jsonResponse.has("selectedProfile") && !jsonResponse.get("selectedProfile").isJsonNull()) {
            JsonObject selectedProfileObj = jsonResponse.getAsJsonObject("selectedProfile");
            selectedProfile = new YggdrasilSession.Profile(
                selectedProfileObj.get("id").getAsString(),
                selectedProfileObj.get("name").getAsString()
            );
        } else if (!availableProfiles.isEmpty()) {
            selectedProfile = availableProfiles.get(0);
        }

        if (selectedProfile == null) {
            throw new IOException("No profile available in refresh response");
        }

        return new YggdrasilSession(newAccessToken, newClientToken, 
            selectedProfile.getId(), selectedProfile.getName(),
            availableProfiles, selectedProfile);
    }

    public boolean validate(String accessToken, String clientToken) throws IOException {
        String baseUrl = authServerUrl.endsWith("/") ? authServerUrl.substring(0, authServerUrl.length() - 1) : authServerUrl;
        URL url = new URL(baseUrl + "/authserver/validate");
        log.debug("Validation URL: " + url.toString() + " / 验证 URL: " + url.toString());
        
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JsonObject request = new JsonObject();
        request.addProperty("accessToken", accessToken);
        if (clientToken != null) {
            request.addProperty("clientToken", clientToken);
        }

        log.debug("Validation request body: " + request.toString().replace(accessToken, "***") + " / 验证请求体: " + request.toString().replace(accessToken, "***"));
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(request.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        log.debug("Validation response code: " + responseCode + " / 验证响应码: " + responseCode);
        
        if (responseCode == 204) {
            // 204 No Content means valid
            log.debug("Token validation successful: Server returned 204 No Content / Token 验证成功: 服务器返回 204 No Content");
            return true;
        } else {
            // Read error response
            String errorResponse = readErrorResponse(conn);
            log.warn("Token validation failed: Server returned " + responseCode + " / Token 验证失败: 服务器返回 " + responseCode);
            if (errorResponse != null && !errorResponse.isEmpty()) {
                log.warn("Error response: " + errorResponse + " / 错误响应: " + errorResponse);
            }
            return false;
        }
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        try (InputStream is = conn.getInputStream();
             java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String readErrorResponse(HttpURLConnection conn) {
        try (InputStream is = conn.getErrorStream()) {
            if (is != null) {
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                String error = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                try {
                    JsonObject errorJson = JsonParser.parseString(error).getAsJsonObject();
                    if (errorJson.has("errorMessage")) {
                        return errorJson.get("errorMessage").getAsString();
                    }
                } catch (Exception e) {
                    // Not JSON, return as-is
                }
                return error;
            }
        } catch (IOException e) {
            log.debug("Failed to read error response", e);
        }
        return "Unknown error";
    }
}

