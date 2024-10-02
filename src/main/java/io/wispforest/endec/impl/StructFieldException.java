package io.wispforest.endec.impl;

public class StructFieldException extends IllegalStateException {
    public StructFieldException(String message, Throwable cause) {
        super(message, cause);
    }

    public static StructFieldException of(String fieldName, Throwable e, boolean encoding) {
        return new StructFieldException("Exception occurred when " + (encoding ? "encoding" : "decoding") + " a given StructField: [Field: " + fieldName + "]", e);
    }
}
