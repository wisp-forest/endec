package io.wispforest.endec.impl;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.Serializer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public record OptionalEndec<T>(Endec<Optional<T>> endec, Supplier<T> defaultValue, boolean mayOmitForField) implements Endec<T> {

    public OptionalEndec(Endec<Optional<T>> endec, Supplier<T> defaultValue){
        this(endec, defaultValue, false);
    }

    @Override
    public void encode(SerializationContext ctx, Serializer<?> serializer, T value) {
        endec.encode(ctx, serializer, Optional.ofNullable(value));
    }

    @Override
    public T decode(SerializationContext ctx, Deserializer<?> deserializer) {
        return endec.decode(ctx, deserializer).orElseGet(defaultValue);
    }

    @Override
    public <S> StructField<S, T> fieldOf(String name, Function<S, T> getter) {
        return mayOmitForField ? Endec.super.optionalFieldOf(name, getter, defaultValue) : Endec.super.fieldOf(name, getter);
    }
}
