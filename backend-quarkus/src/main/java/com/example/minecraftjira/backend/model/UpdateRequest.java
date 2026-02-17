package com.example.minecraftjira.backend.model;

public record UpdateRequest(String item, String newStatus, String text, String actor) {
}
