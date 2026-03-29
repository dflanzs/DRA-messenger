package com.tfg.backend.Config;

public class WebSocketAuthException extends RuntimeException {
    private final String code;

    public WebSocketAuthException(String message, String code) {
        super(message);
        this.code = code;
    }


    public WebSocketAuthException(String message, String code, Throwable cause) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
