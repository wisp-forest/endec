package io.wispforest.endec;

import io.wispforest.endec.impl.StructField;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Marker and template interface for all endecs which serialize structs
 * <p>
 * Every such endec should extend this interface to profit from the implementation of {@link #mapCodec(SerializationAttribute...)}
 * and composability which allows {@link Endec#dispatchedStruct(Function, Function, Endec, String)} to work
 */
public interface StructEndec<T> extends Endec<T> {

    void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, T value);

    T decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct);

    @Override
    default void encode(SerializationContext ctx, Serializer<?> serializer, T value) {
        try (var struct = serializer.struct()) {
            this.encodeStruct(ctx, serializer, struct, value);
        }
    }

    @Override
    default T decode(SerializationContext ctx, Deserializer<?> deserializer) {
        return this.decodeStruct(ctx, deserializer, deserializer.struct());
    }

    default <S> StructField<S, T> flatFieldOf(Function<S, T> getter) {
        return new StructField.Flat<>(this, getter);
    }

    @Override
    default <R> StructEndec<R> xmap(Function<T, R> to, Function<R, T> from) {
        return new StructEndec<>() {
            @Override
            public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, R value) {
                StructEndec.this.encodeStruct(ctx, serializer, struct, from.apply(value));
            }
            @Override
            public R decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
                return to.apply(StructEndec.this.decodeStruct(ctx, deserializer, struct));
            }
        };
    }

    @Override
    default <R> StructEndec<R> xmapWithContext(BiFunction<SerializationContext, T, R> to, BiFunction<SerializationContext, R, T> from) {
        return new StructEndec<>() {
            @Override
            public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, R value) {
                StructEndec.this.encodeStruct(ctx, serializer, struct, from.apply(ctx, value));
            }
            @Override
            public R decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
                return to.apply(ctx, StructEndec.this.decodeStruct(ctx, deserializer, struct));
            }
        };
    }
}
