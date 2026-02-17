package com.example.minecraftjira.backend.auth;

import com.example.minecraftjira.backend.model.ApiResponse;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {
    @ConfigProperty(name = "minecraft.api-key")
    String configuredApiKey;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        if ("health".equals(normalizedPath) || normalizedPath.startsWith("q/")) {
            return;
        }

        String providedKey = requestContext.getHeaderString("X-API-Key");
        if (!isValid(providedKey)) {
            requestContext.abortWith(Response
                    .status(Response.Status.UNAUTHORIZED)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(ApiResponse.error("Unauthorized: missing or invalid X-API-Key"))
                    .build());
        }
    }

    private boolean isValid(String providedKey) {
        if (providedKey == null || providedKey.isBlank()) {
            return false;
        }

        byte[] expected = configuredApiKey.getBytes(StandardCharsets.UTF_8);
        byte[] received = providedKey.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, received);
    }
}
