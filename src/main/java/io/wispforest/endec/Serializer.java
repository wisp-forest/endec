package io.wispforest.endec;


import io.wispforest.endec.data.SerializationContext;
import io.wispforest.endec.util.Endable;

import java.util.Optional;

public interface Serializer<T> {

    default SerializationContext initalContext(SerializationContext ctx) {
        return ctx;
    }

    void writeByte(SerializationContext ctx, byte value);
    void writeShort(SerializationContext ctx, short value);
    void writeInt(SerializationContext ctx, int value);
    void writeLong(SerializationContext ctx, long value);
    void writeFloat(SerializationContext ctx, float value);
    void writeDouble(SerializationContext ctx, double value);

    void writeVarInt(SerializationContext ctx, int value);
    void writeVarLong(SerializationContext ctx, long value);

    void writeBoolean(SerializationContext ctx, boolean value);
    void writeString(SerializationContext ctx, String value);
    void writeBytes(SerializationContext ctx, byte[] bytes);

    <V> void writeOptional(SerializationContext ctx, Endec<V> endec, Optional<V> optional);

    <E> Sequence<E> sequence(SerializationContext ctx, Endec<E> elementEndec, int size);
    <V> Map<V> map(SerializationContext ctx, Endec<V> valueEndec, int size);
    Struct struct();

    T result();

    interface Sequence<E> extends Endable {
        void element(E element);
    }

    interface Map<V> extends Endable {
        void entry(String key, V value);
    }

    interface Struct extends Endable {
        <F> Struct field(SerializationContext ctx, String name, Endec<F> endec, F value);
    }
}
