package com.example.triageclient.service;

public class TriageApiException extends RuntimeException {
    public TriageApiException(String message) {
        super(message);
    }

    public TriageApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
