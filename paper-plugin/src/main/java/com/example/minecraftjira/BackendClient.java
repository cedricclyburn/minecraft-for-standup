package com.example.minecraftjira;

import com.example.minecraftjira.model.BackendResponse;
import com.example.minecraftjira.model.StandupRequest;
import com.example.minecraftjira.model.UpdateRequest;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class BackendClient {
    private final JavaPlugin plugin;
    private final String baseUrl;
    private final String apiKey;
    private final Duration requestTimeout;
    private final HttpClient httpClient;
    private final Gson gson;

    public BackendClient(JavaPlugin plugin, String baseUrl, String apiKey, int connectTimeoutMs, int requestTimeoutMs) {
        this.plugin = plugin;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.requestTimeout = Duration.ofMillis(requestTimeoutMs);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        this.gson = new Gson();
    }

    public CompletableFuture<BackendResponse> getHelp() {
        return sendGet("/jira/help");
    }

    public CompletableFuture<BackendResponse> getStatus(String item) {
        String encodedItem = URLEncoder.encode(item, StandardCharsets.UTF_8);
        return sendGet("/jira/status?item=" + encodedItem);
    }

    public CompletableFuture<BackendResponse> update(UpdateRequest request) {
        return sendPost("/jira/update", request);
    }

    public CompletableFuture<BackendResponse> standup(StandupRequest request) {
        return sendPost("/jira/standup", request);
    }

    private CompletableFuture<BackendResponse> sendGet(String path) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("X-API-Key", apiKey)
                .header("Accept", "application/json")
                .timeout(requestTimeout)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseResponse);
    }

    private CompletableFuture<BackendResponse> sendPost(String path, Object body) {
        String payload = gson.toJson(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("X-API-Key", apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(requestTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseResponse);
    }

    private BackendResponse parseResponse(HttpResponse<String> response) {
        try {
            BackendResponse parsed = gson.fromJson(response.body(), BackendResponse.class);
            if (parsed == null) {
                return BackendResponse.error("Backend returned an empty response body");
            }
            if (response.statusCode() >= 200 && response.statusCode() <= 299) {
                return parsed;
            }
            String message = parsed.getMessage() != null ? parsed.getMessage() : "Backend returned HTTP " + response.statusCode();
            return BackendResponse.error(message);
        } catch (JsonSyntaxException e) {
            plugin.getLogger().warning("Unable to parse backend JSON response: " + e.getMessage());
            String message = "Backend returned HTTP " + response.statusCode() + " with non-JSON body";
            return BackendResponse.error(message);
        }
    }
}
