package io.wispforest.endec.util;

import io.wispforest.endec.Serializer;
import io.wispforest.endec.data.SerializationContext;

public interface ContextedEncoder<T, D> {
    void encodeWith(SerializationContext ctx, Serializer<?> serializer, D data, T value);
}
