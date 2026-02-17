package com.example.minecraftjira.backend.model;

public record UpdateData(String item, String previousStatus, String newStatus, String source) {
}
