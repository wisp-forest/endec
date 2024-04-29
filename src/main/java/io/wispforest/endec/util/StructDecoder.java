package io.wispforest.endec.util;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.data.SerializationContext;

public interface StructDecoder<T> {
    T decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct);
}
