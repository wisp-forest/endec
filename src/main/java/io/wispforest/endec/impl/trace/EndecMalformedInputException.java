package io.wispforest.endec.impl.trace;

public class EndecMalformedInputException extends RuntimeException {

    public final EndecTrace location;
    public final String message;

    public EndecMalformedInputException(EndecTrace location, String message) {
        super(createMessage(location, message));

        this.location = location;
        this.message = message;
    }

    private static String createMessage(EndecTrace location, String message) {
        return "Malformed input at " + location.toString() + ": " + message;
    }
}
