package com.example.minecraftjira.model;

import com.google.gson.JsonObject;

public class BackendResponse {
    private boolean ok;
    private String message;
    private JsonObject data;

    public BackendResponse() {
    }

    public BackendResponse(boolean ok, String message, JsonObject data) {
        this.ok = ok;
        this.message = message;
        this.data = data;
    }

    public boolean isOk() {
        return ok;
    }

    public String getMessage() {
        return message;
    }

    public JsonObject getData() {
        return data;
    }

    public static BackendResponse error(String message) {
        return new BackendResponse(false, message, null);
    }
}
