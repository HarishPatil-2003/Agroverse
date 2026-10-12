package com.agroverse.backend.dto;

public class ChatRequest {
    private String message;
    private String language;
    private java.util.Map<String, Object> context;

    public ChatRequest() {}

    public ChatRequest(String message, String language) {
        this.message = message;
        this.language = language;
    }

    public ChatRequest(String message, String language, java.util.Map<String, Object> context) {
        this.message = message;
        this.language = language;
        this.context = context;
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

    public java.util.Map<String, Object> getContext() {
        return context;
    }

    public void setContext(java.util.Map<String, Object> context) {
        this.context = context;
    }
}
