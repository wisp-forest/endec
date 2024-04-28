package io.wispforest.endec;


import io.wispforest.endec.data.DataToken;
import io.wispforest.endec.data.ExtraDataContext;
import io.wispforest.endec.format.forwarding.ForwardingSerializer;
import io.wispforest.endec.util.Endable;

import java.util.Optional;

public interface Serializer<T> {

    default ExtraDataContext initalContext(ExtraDataContext ctx) {
        return ctx;
    }

    void writeByte(ExtraDataContext ctx, byte value);
    void writeShort(ExtraDataContext ctx, short value);
    void writeInt(ExtraDataContext ctx, int value);
    void writeLong(ExtraDataContext ctx, long value);
    void writeFloat(ExtraDataContext ctx, float value);
    void writeDouble(ExtraDataContext ctx, double value);

    void writeVarInt(ExtraDataContext ctx, int value);
    void writeVarLong(ExtraDataContext ctx, long value);

    void writeBoolean(ExtraDataContext ctx, boolean value);
    void writeString(ExtraDataContext ctx, String value);
    void writeBytes(ExtraDataContext ctx, byte[] bytes);

    <V> void writeOptional(ExtraDataContext ctx, Endec<V> endec, Optional<V> optional);

    <E> Sequence<E> sequence(ExtraDataContext ctx, Endec<E> elementEndec, int size);
    <V> Map<V> map(ExtraDataContext ctx, Endec<V> valueEndec, int size);
    Struct struct();

    T result();

    interface Sequence<E> extends Endable {
        void element(E element);
    }

    interface Map<V> extends Endable {
        void entry(String key, V value);
    }

    interface Struct extends Endable {
        <F> Struct field(ExtraDataContext ctx, String name, Endec<F> endec, F value);
    }
}
