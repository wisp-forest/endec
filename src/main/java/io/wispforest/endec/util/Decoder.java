package io.wispforest.endec.util;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.data.SerializationContext;

public interface Decoder<T> {
    T decode(SerializationContext ctx, Deserializer<?> deserializer);
}
