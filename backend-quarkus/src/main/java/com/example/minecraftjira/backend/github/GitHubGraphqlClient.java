package com.example.minecraftjira.backend.github;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@ApplicationScoped
public class GitHubGraphqlClient {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Gson gson = new Gson();

    @ConfigProperty(name = "github.graphql-url")
    String graphQlUrl;

    @ConfigProperty(name = "github.token", defaultValue = "")
    String githubToken;

    public JsonObject execute(String query, JsonObject variables) {
        JsonObject payload = new JsonObject();
        payload.addProperty("query", query);
        payload.add("variables", variables);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(graphQlUrl))
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github+json")
                .header("Content-Type", "application/json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GitHub GraphQL request failed", e);
        } catch (IOException e) {
            throw new IllegalStateException("GitHub GraphQL request failed", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("GitHub GraphQL returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        JsonArray errors = root.getAsJsonArray("errors");
        if (errors != null && !errors.isEmpty()) {
            throw new IllegalStateException("GitHub GraphQL errors: " + errors);
        }

        return root;
    }
}
