package io.wispforest.endec.util;

import io.wispforest.endec.Serializer;
import io.wispforest.endec.data.SerializationContext;

public interface ContextedStructEncoder<T, C> {
    void encodeStructWith(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, C data, T value);
}
