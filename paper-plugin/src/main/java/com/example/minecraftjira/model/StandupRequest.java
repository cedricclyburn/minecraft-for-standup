package com.example.minecraftjira.model;

public class StandupRequest {
    private final String prompt;
    private final String actor;

    public StandupRequest(String prompt, String actor) {
        this.prompt = prompt;
        this.actor = actor;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getActor() {
        return actor;
    }
}
