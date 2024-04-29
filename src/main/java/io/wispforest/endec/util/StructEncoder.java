package io.wispforest.endec.util;

import io.wispforest.endec.Serializer;
import io.wispforest.endec.data.SerializationContext;

public interface StructEncoder<T> {
    void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, T value);
}
