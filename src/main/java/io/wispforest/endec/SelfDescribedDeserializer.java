package io.wispforest.endec;

import io.wispforest.endec.data.SerializationContext;

public interface SelfDescribedDeserializer<T> extends Deserializer<T> {
    <S> void readAny(SerializationContext ctx, Serializer<S> visitor);
}
