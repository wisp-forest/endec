package io.wispforest.endec;

public interface SelfDescribedDeserializer<T> extends Deserializer<T> {
    <S> void readAny(Serializer<S> visitor);
}
