package io.wispforest.endec.impl;

public class MissingTokenDataException extends RuntimeException {

    public MissingTokenDataException() {
        super();
    }

    public MissingTokenDataException(String message) {
        super(message);
    }

    public MissingTokenDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingTokenDataException(Throwable cause) {
        super(cause);
    }

    protected MissingTokenDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
