package io.wispforest.endec.impl;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.Serializer;

import java.util.function.UnaryOperator;

public class RecursiveEndec<T> implements Endec<T> {

    public final Endec<T> endec;

    public RecursiveEndec(UnaryOperator<Endec<T>> builder) {
        this.endec = builder.apply(this);
    }

    @Override
    public void encode(SerializationContext ctx, Serializer<?> serializer, T value) {
        this.endec.encode(ctx, serializer, value);
    }

    @Override
    public T decode(SerializationContext ctx, Deserializer<?> deserializer) {
        return this.endec.decode(ctx, deserializer);
    }
}
