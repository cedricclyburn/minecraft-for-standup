package com.example.minecraftjira.backend.model;

import java.util.List;

public record StandupData(String summary, List<String> blockers, String source) {
}
