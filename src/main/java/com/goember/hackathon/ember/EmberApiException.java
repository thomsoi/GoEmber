package com.goember.hackathon.ember;

public class EmberApiException extends RuntimeException {
    private final boolean timeout;

    public EmberApiException(String message, Throwable cause, boolean timeout) {
        super(message, cause);
        this.timeout = timeout;
    }

    public boolean isTimeout() { return timeout; }
}
