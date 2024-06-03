package io.wispforest.endec;

public interface SelfDescribedDeserializer<T> extends Deserializer<T> {
    <S> void readAny(SerializationContext ctx, Serializer<S> visitor);
}
