package io.wispforest.endec;

import io.wispforest.endec.data.ExtraDataContext;

public interface SelfDescribedDeserializer<T> extends Deserializer<T> {
    <S> void readAny(Serializer<S> visitor, ExtraDataContext ctx);
}
