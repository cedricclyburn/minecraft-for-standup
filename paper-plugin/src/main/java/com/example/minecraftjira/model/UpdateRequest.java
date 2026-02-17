package com.example.minecraftjira.model;

public class UpdateRequest {
    private final String item;
    private final String newStatus;
    private final String text;
    private final String actor;

    public UpdateRequest(String item, String newStatus, String text, String actor) {
        this.item = item;
        this.newStatus = newStatus;
        this.text = text;
        this.actor = actor;
    }

    public String getItem() {
        return item;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public String getText() {
        return text;
    }

    public String getActor() {
        return actor;
    }
}
