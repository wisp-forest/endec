package io.wispforest.endec;

import io.wispforest.endec.data.ExtraDataContext;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;

public interface Deserializer<T> {

    default ExtraDataContext initalContext(ExtraDataContext ctx) {
        return ctx;
    }

    byte readByte(ExtraDataContext ctx);
    short readShort(ExtraDataContext ctx);
    int readInt(ExtraDataContext ctx);
    long readLong(ExtraDataContext ctx);
    float readFloat(ExtraDataContext ctx);
    double readDouble(ExtraDataContext ctx);

    int readVarInt(ExtraDataContext ctx);
    long readVarLong(ExtraDataContext ctx);

    boolean readBoolean(ExtraDataContext ctx);
    String readString(ExtraDataContext ctx);
    byte[] readBytes(ExtraDataContext ctx);
    <V> Optional<V> readOptional(ExtraDataContext ctx, Endec<V> endec);

    <E> Sequence<E> sequence(ExtraDataContext ctx, Endec<E> elementEndec);
    <V> Map<V> map(ExtraDataContext ctx, Endec<V> valueEndec);
    Struct struct();

    <V> V tryRead(BiFunction<Deserializer<T>, ExtraDataContext, V> reader, ExtraDataContext ctx);

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
        <F> @Nullable F field(ExtraDataContext ctx, String name, Endec<F> endec);

        /**
         * Decode the value of field {@code name} using {@code endec}. If no
         * such field exists in the serialized data, {@code defaultValue} is returned
         */
        <F> @Nullable F field(ExtraDataContext ctx, String name, Endec<F> endec, @Nullable F defaultValue);
    }
}
