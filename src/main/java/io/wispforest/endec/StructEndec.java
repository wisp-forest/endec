package io.wispforest.endec;

import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.endec.impl.StructField;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Marker and template interface for all endecs which serialize structs
 * <p>
 * Every such endec should extend this interface to profit from the implementation of {@link #mapCodec(SerializationAttribute...)}
 * and composability which allows {@link Endec#dispatchedStruct(Function, Function, Endec, String)} to work
 */
public interface StructEndec<T> extends Endec<T> {

    void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, T value);

    T decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct);

    /**
     * Static constructor for {@link StructEndec} for use when base use of such is desired, it is recommended that
     * you use {@link StructEndecBuilder} as encoding and decoding of data must be kept
     * in the same order with same field names used across both encoding and decoding or issues may arise for
     * formats that are not Self Describing.
     */
    static <T> StructEndec<T> of(StructuredEncoder<T> encoder, StructuredDecoder<T> decoder) {
        return new StructEndec<T>() {
            @Override
            public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, T value) {
                encoder.encodeStruct(ctx, serializer, struct, value);
            }

            @Override
            public T decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
                return decoder.decodeStruct(ctx, deserializer, struct);
            }
        };
    }

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

    /**
     * @deprecated Use {@link Endec#unit(Object)}
     */
    @Deprecated(forRemoval = true)
    static <T> StructEndec<T> unit(T instance) {
        return Endec.unit(instance);
    }

    /**
     * @deprecated Use {@link Endec#unit(Supplier)}
     */
    @Deprecated(forRemoval = true)
    static <T> StructEndec<T> unit(Supplier<T> instance) {
        return Endec.unit(instance);
    }

    default <S> StructField<S, T> flatFieldOf(Function<S, T> getter) {
        return new StructField.Flat<>(this, getter);
    }

    @Override
    default <R> StructEndec<R> xmap(Function<T, R> to, Function<R, T> from) {
        return StructEndec.of(
                (ctx, serializer, struct, value) -> StructEndec.this.encodeStruct(ctx, serializer, struct, from.apply(value)),
                (ctx, deserializer, struct) -> to.apply(StructEndec.this.decodeStruct(ctx, deserializer, struct))
        );
    }

    @Override
    default <R> StructEndec<R> xmapWithContext(BiFunction<SerializationContext, T, R> to, BiFunction<SerializationContext, R, T> from) {
        return StructEndec.of(
                (ctx, serializer, struct, value) -> StructEndec.this.encodeStruct(ctx, serializer, struct, from.apply(ctx, value)),
                (ctx, deserializer, struct) -> to.apply(ctx, StructEndec.this.decodeStruct(ctx, deserializer, struct))
        );
    }

    default StructEndec<T> structuredCatchErrors(StructuredDecoderWithError<T> decodeOnError) {
        return StructEndec.of(this::encodeStruct, (ctx, deserializer, struct) -> {
            try {
                return deserializer.tryRead(deserializer1 -> this.decodeStruct(ctx, deserializer1, struct));
            } catch (Exception e) {
                return decodeOnError.decodeStruct(ctx, deserializer, struct, e);
            }
        });
    }

    @Override
    default StructEndec<T> validate(Consumer<T> validator) {
        return this.xmap(t -> {
            validator.accept(t);
            return t;
        }, t -> {
            validator.accept(t);
            return t;
        });
    }

    @FunctionalInterface
    interface StructuredEncoder<T> {
        void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, T value);
    }

    @FunctionalInterface
    interface StructuredDecoder<T> {
        T decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct);
    }

    @FunctionalInterface
    interface StructuredDecoderWithError<T> {
        T decodeStruct(SerializationContext ctx, Deserializer<?> serializer, Deserializer.Struct struct, Exception exception);
    }
}
