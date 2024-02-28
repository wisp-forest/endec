package io.wispforest.endec.util;

import io.wispforest.endec.Endec;

public interface EndecBuffer {
    default <T> void write(Endec<T> endec, T value) {
        throw new UnsupportedOperationException();
    }

    default <T> T read(Endec<T> endec) {
        throw new UnsupportedOperationException();
    }
}
