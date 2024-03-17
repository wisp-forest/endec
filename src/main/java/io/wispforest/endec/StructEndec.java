package io.wispforest.endec;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Marker and template interface for all endecs which serialize structs
 * <p>
 * Every such endec should extend this interface to profit from the implementation of {@link #mapCodec(SerializationAttribute...)}
 * and composability which allows {@link Endec#dispatchedStruct(Function, Function, Endec, String)} to work
 */
public interface StructEndec<T> extends Endec<T> {

    void encodeStruct(Serializer<?> serializer, Serializer.Struct struct, T value);

    T decodeStruct(Deserializer<?> deserializer, Deserializer.Struct struct);

    @Override
    default void encode(Serializer<?> serializer, T value) {
        try (var struct = serializer.struct()) {
            this.encodeStruct(serializer, struct, value);
        }
    }

    @Override
    default T decode(Deserializer<?> deserializer) {
        return this.decodeStruct(deserializer, deserializer.struct());
    }


    @Override
    default <D, R> StructEndec<R> ofToken(DataToken<D> attribute, BiFunction<D, T, R> to, BiFunction<D, R, T> from) {
        return new StructEndec<R>() {
            @Override
            public void encodeStruct(Serializer<?> serializer, Serializer.Struct struct, R value) {
                StructEndec.this.encodeStruct(serializer, struct, from.apply(serializer.getOrThrow(attribute), value));
            }

            @Override
            public R decodeStruct(Deserializer<?> deserializer, Deserializer.Struct struct) {
                return to.apply(deserializer.getOrThrow(attribute), StructEndec.this.decodeStruct(deserializer, struct));
            }
        };
    }

    @Override
    default <R> StructEndec<R> xmap(Function<T, R> to, Function<R, T> from) {
        return new StructEndec<R>() {
            @Override
            public void encodeStruct(Serializer<?> serializer, Serializer.Struct struct, R value) {
                StructEndec.this.encodeStruct(serializer, struct, from.apply(value));
            }

            @Override
            public R decodeStruct(Deserializer<?> deserializer, Deserializer.Struct struct) {
                return to.apply(StructEndec.this.decodeStruct(deserializer, struct));
            }
        };
    }
}
