package io.wispforest.endec.impl;

public class SuppressedTokenDataException extends RuntimeException {

    public SuppressedTokenDataException() {
        super();
    }

    public SuppressedTokenDataException(String message) {
        super(message);
    }

    public SuppressedTokenDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public SuppressedTokenDataException(Throwable cause) {
        super(cause);
    }

    protected SuppressedTokenDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}