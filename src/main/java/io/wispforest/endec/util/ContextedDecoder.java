package io.wispforest.endec.util;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.data.SerializationContext;

public interface ContextedDecoder<T, D> {
    T decodeWith(SerializationContext ctx, Deserializer<?> deserializer, D data);
}
