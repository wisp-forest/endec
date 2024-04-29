package io.wispforest.endec.util;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.data.SerializationContext;

public interface ContextedStructDecoder<T, C> {
    T decodeStructWith(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct, C data);
}
