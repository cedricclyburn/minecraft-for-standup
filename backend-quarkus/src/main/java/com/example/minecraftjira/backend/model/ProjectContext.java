package com.example.minecraftjira.backend.model;

import java.util.Map;

public record ProjectContext(String projectId, String statusFieldId, Map<String, String> statusOptionIds) {
}
