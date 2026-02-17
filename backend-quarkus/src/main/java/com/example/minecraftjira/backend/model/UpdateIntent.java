package com.example.minecraftjira.backend.model;

public record UpdateIntent(String action, String item, String status, String rawText) {
}
