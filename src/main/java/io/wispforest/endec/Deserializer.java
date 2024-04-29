package io.wispforest.endec;

import io.wispforest.endec.data.SerializationContext;
import io.wispforest.endec.util.Decoder;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Optional;

public interface Deserializer<T> {

    default SerializationContext setupContext(SerializationContext ctx) {
        return ctx;
    }

    byte readByte(SerializationContext ctx);
    short readShort(SerializationContext ctx);
    int readInt(SerializationContext ctx);
    long readLong(SerializationContext ctx);
    float readFloat(SerializationContext ctx);
    double readDouble(SerializationContext ctx);

    int readVarInt(SerializationContext ctx);
    long readVarLong(SerializationContext ctx);

    boolean readBoolean(SerializationContext ctx);
    String readString(SerializationContext ctx);
    byte[] readBytes(SerializationContext ctx);
    <V> Optional<V> readOptional(SerializationContext ctx, Endec<V> endec);

    <E> Sequence<E> sequence(SerializationContext ctx, Endec<E> elementEndec);
    <V> Map<V> map(SerializationContext ctx, Endec<V> valueEndec);
    Struct struct();

    <V> V tryRead(SerializationContext ctx, Decoder<V> reader);

    interface Sequence<E> extends Iterator<E> {

        int estimatedSize();

        @Override
        boolean hasNext();

        @Override
        E next();
    }

    interface Map<E> extends Iterator<java.util.Map.Entry<String, E>> {

        int estimatedSize();

        @Override
        boolean hasNext();

        @Override
        java.util.Map.Entry<String, E> next();
    }

    interface Struct {
        /**
         * Decode the value of field {@code name} using {@code endec}. If no
         * such field exists in the serialized data, an exception is thrown
         */
        <F> @Nullable F field(SerializationContext ctx, String name, Endec<F> endec);

        /**
         * Decode the value of field {@code name} using {@code endec}. If no
         * such field exists in the serialized data, {@code defaultValue} is returned
         */
        <F> @Nullable F field(SerializationContext ctx, String name, Endec<F> endec, @Nullable F defaultValue);
    }
}
