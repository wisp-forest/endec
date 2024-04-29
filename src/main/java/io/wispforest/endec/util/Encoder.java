package io.wispforest.endec.util;

import io.wispforest.endec.Serializer;
import io.wispforest.endec.data.SerializationContext;

public interface Encoder<T> {
    void encode(SerializationContext ctx, Serializer<?> serializer, T value);
}
