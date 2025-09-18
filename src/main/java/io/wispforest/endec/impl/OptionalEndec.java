package io.wispforest.endec.impl;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.Serializer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public record OptionalEndec<T>(Endec<Optional<T>> endec, Supplier<T> defaultValue, @Nullable Predicate<T> isDefaultValue, boolean mayOmitForField) implements Endec<T> {

    public OptionalEndec(Endec<Optional<T>> endec, Supplier<T> defaultValue, @Nullable Predicate<T> isDefaultValue){
        this(endec, defaultValue, isDefaultValue, false);
    }

    private Optional<T> getOptional(T value) {
        return isDefaultValue != null && isDefaultValue.test(value) ? Optional.empty() : Optional.ofNullable(value);
    }

    @Override
    public void encode(SerializationContext ctx, Serializer<?> serializer, T value) {
        endec.encode(ctx, serializer, getOptional(value));
    }

    @Override
    public T decode(SerializationContext ctx, Deserializer<?> deserializer) {
        return endec.decode(ctx, deserializer).orElseGet(defaultValue);
    }

    @Override
    public <S> StructField<S, T> fieldOf(String name, Function<S, T> getter) {
        return mayOmitForField ? new StructField<>(name, this, getter, defaultValue) : Endec.super.fieldOf(name, getter);
    }
}
