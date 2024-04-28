package io.wispforest.endec;

import io.wispforest.endec.data.DataToken;
import io.wispforest.endec.data.ExtraDataContext;
import io.wispforest.endec.impl.StructField;
import io.wispforest.endec.util.QuadFunction;
import io.wispforest.endec.util.QuintaConsumer;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Marker and template interface for all endecs which serialize structs
 * <p>
 * Every such endec should extend this interface to profit from the implementation of {@link #mapCodec(SerializationAttribute...)}
 * and composability which allows {@link Endec#dispatchedStruct(Function, Function, Endec, String)} to work
 */
public interface StructEndec<T> extends Endec<T> {

    void encodeStruct(Serializer<?> serializer, Serializer.Struct struct, ExtraDataContext ctx, T value);

    T decodeStruct(Deserializer<?> deserializer, Deserializer.Struct struct, ExtraDataContext ctx);

    @Override
    default void encode(Serializer<?> serializer, ExtraDataContext ctx, T value) {
        try (var struct = serializer.struct()) {
            this.encodeStruct(serializer, struct, ctx, value);
        }
    }

    @Override
    default T decode(Deserializer<?> deserializer, ExtraDataContext ctx) {
        return this.decodeStruct(deserializer, deserializer.struct(), ctx);
    }

    default <S> StructField<S, T> flatFieldOf(Function<S, T> getter) {
        return new StructField.Flat<>(this, getter);
    }

    @Override
    default <D, R> StructEndec<R> ofToken(DataToken.Instanced<D> attribute, BiFunction<D, T, R> to, BiFunction<D, R, T> from) {
        return StructEndec.ofTokenStruct(attribute,
                (serializer, struct, context, d, r) -> StructEndec.this.encodeStruct(serializer, struct, context, from.apply(d, r)),
                (deserializer, struct, context, d) -> to.apply(d, StructEndec.this.decodeStruct(deserializer, struct, context)));
    }

    static <R, D> StructEndec<R> ofTokenStruct(DataToken.Instanced<D> token, QuintaConsumer<Serializer<?>, Serializer.Struct, ExtraDataContext, D, R> encode, QuadFunction<Deserializer<?>, Deserializer.Struct, ExtraDataContext, D, R> decode){
        return new StructEndec<>() {
            @Override
            public void encodeStruct(Serializer<?> serializer, Serializer.Struct struct, ExtraDataContext ctx, R value) {
                encode.accept(serializer, struct, ctx, ctx.getOrThrow(token), value);
            }

            @Override
            public R decodeStruct(Deserializer<?> deserializer, Deserializer.Struct struct, ExtraDataContext ctx) {
                return decode.apply(deserializer, struct, ctx, ctx.getOrThrow(token));
            }
        };
    }

    @Override
    default <R> StructEndec<R> xmap(Function<T, R> to, Function<R, T> from) {
        return new StructEndec<>() {
            @Override
            public void encodeStruct(Serializer<?> serializer, Serializer.Struct struct, ExtraDataContext ctx, R value) {
                StructEndec.this.encodeStruct(serializer, struct, ctx, from.apply(value));
            }

            @Override
            public R decodeStruct(Deserializer<?> deserializer, Deserializer.Struct struct, ExtraDataContext ctx) {
                return to.apply(StructEndec.this.decodeStruct(deserializer, struct, ctx));
            }
        };
    }
}
