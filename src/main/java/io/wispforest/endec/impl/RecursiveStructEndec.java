package io.wispforest.endec.impl;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.StructEndec;

import java.util.function.UnaryOperator;

public class RecursiveStructEndec<T> implements StructEndec<T> {

    public final StructEndec<T> structEndec;

    public RecursiveStructEndec(UnaryOperator<StructEndec<T>> builder) {
        this.structEndec = builder.apply(this);
    }

    @Override
    public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, T value) {
        this.structEndec.encodeStruct(ctx, serializer, struct, value);
    }

    @Override
    public T decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
        return this.structEndec.decodeStruct(ctx, deserializer, struct);
    }
}
