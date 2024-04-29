package io.wispforest.endec.util;

import io.wispforest.endec.Endec;
import io.wispforest.endec.data.SerializationContext;

public interface EndecBuffer {
    default <T> void write(SerializationContext ctx, Endec<T> endec, T value) {
        throw new UnsupportedOperationException();
    }

    default <T> T read(SerializationContext ctx, Endec<T> endec) {
        throw new UnsupportedOperationException();
    }
}
