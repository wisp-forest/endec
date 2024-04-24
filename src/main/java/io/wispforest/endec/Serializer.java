package io.wispforest.endec;


import io.wispforest.endec.data.DataToken;
import io.wispforest.endec.data.ExtraDataContext;
import io.wispforest.endec.format.forwarding.ForwardingSerializer;
import io.wispforest.endec.util.Endable;

import java.util.Optional;

public interface Serializer<T> extends ExtraDataContext {

    default Serializer<T> withTokens(DataToken.Instance ...instances) {
        return ForwardingSerializer.of(this, instances);
    }

    void writeByte(byte value);
    void writeShort(short value);
    void writeInt(int value);
    void writeLong(long value);
    void writeFloat(float value);
    void writeDouble(double value);

    void writeVarInt(int value);
    void writeVarLong(long value);

    void writeBoolean(boolean value);
    void writeString(String value);
    void writeBytes(byte[] bytes);
    <V> void writeOptional(Endec<V> endec, Optional<V> optional);

    <E> Sequence<E> sequence(Endec<E> elementEndec, int size);
    <V> Map<V> map(Endec<V> valueEndec, int size);
    Struct struct();

    T result();

    interface Sequence<E> extends Endable {
        void element(E element);
    }

    interface Map<V> extends Endable {
        void entry(String key, V value);
    }

    interface Struct extends Endable {
        <F> Struct field(String name, Endec<F> endec, F value);
    }
}
