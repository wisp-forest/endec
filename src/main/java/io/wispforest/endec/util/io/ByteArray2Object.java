package io.wispforest.endec.util.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public interface ByteArray2Object<T> {

    static <T> ByteArray2Object<T> fromString(Function<String, T> func) {
        return bytes -> func.apply(new String(bytes, StandardCharsets.UTF_8));
    }

    T from(byte[] bytes) throws IOException;
}
