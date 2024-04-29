package io.wispforest.endec;

import io.wispforest.endec.data.DataToken;
import io.wispforest.endec.data.SerializationContext;
import io.wispforest.endec.impl.StructField;
import io.wispforest.endec.util.ContextedStructDecoder;
import io.wispforest.endec.util.ContextedStructEncoder;
import io.wispforest.endec.util.StructDecoder;
import io.wispforest.endec.util.StructEncoder;

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

    static <R> StructEndec<R> of(StructEncoder<R> encoder, StructDecoder<R> decoder) {
        return new StructEndec<R>() {
            @Override
            public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, R value) {
                encoder.encodeStruct(ctx, serializer, struct, value);
            }

            @Override
            public R decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
                return decoder.decodeStruct(ctx, deserializer, struct);
            }
        };
    }

    @Override
    default <D, R> StructEndec<R> ofToken(DataToken.Instanced<D> attribute, BiFunction<D, T, R> to, BiFunction<D, R, T> from) {
        return ofTokenStruct(attribute,
                (ctx, serializer, struct, d, r) -> StructEndec.this.encodeStruct(ctx, serializer, struct, from.apply(d, r)),
                (ctx, deserializer, struct, d) -> to.apply(d, StructEndec.this.decodeStruct(ctx, deserializer, struct)));
    }

    static <R, C> StructEndec<R> ofTokenStruct(DataToken.Instanced<C> token, ContextedStructEncoder<R, C> encode, ContextedStructDecoder<R, C> decode){
        return StructEndec.of(
                (ctx, serializer, struct, value) -> encode.encodeStructWith(ctx, serializer, struct, ctx.getOrThrow(token), value),
                (ctx, deserializer, struct) -> decode.decodeStructWith(ctx, deserializer, struct, ctx.getOrThrow(token)));
    }

    @Override
    default <R> StructEndec<R> xmap(Function<T, R> to, Function<R, T> from) {
        return StructEndec.of(
                (ctx, serializer, struct, value) -> StructEndec.this.encodeStruct(ctx, serializer, struct, from.apply(value)),
                (ctx, deserializer, struct) -> to.apply(StructEndec.this.decodeStruct(ctx, deserializer, struct)));
    }
}
