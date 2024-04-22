package io.wispforest.endec.util.io;

import java.io.IOException;

public interface Object2ByteArray<T> {

    byte[] from(T t) throws IOException;
}
