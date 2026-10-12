package com.agroverse.backend.dto;

public class ChatRequest {
    private String message;
    private String language;

    public ChatRequest() {}

    public ChatRequest(String message, String language) {
        this.message = message;
        this.language = language;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
