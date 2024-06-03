package io.wispforest.endec.impl;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.SerializationContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public sealed class StructField<S, F> permits StructField.Flat {

    protected final String name;
    protected final Endec<F> endec;
    protected final Function<S, F> getter;
    protected final @Nullable Supplier<F> defaultValueFactory;

    public StructField(String name, Endec<F> endec, Function<S, F> getter, @Nullable Supplier<F> defaultValueFactory) {
        this.name = name;
        this.endec = endec;
        this.getter = getter;
        this.defaultValueFactory = defaultValueFactory;
    }

    public StructField(String name, Endec<F> endec, Function<S, F> getter, @Nullable F defaultValue) {
        this(name, endec, getter, () -> defaultValue);
    }

    public StructField(String name, Endec<F> endec, Function<S, F> getter) {
        this(name, endec, getter, (Supplier<F>) null);
    }

    public void encodeField(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, S instance) {
        struct.field(this.name, ctx, this.endec, this.getter.apply(instance));
    }

    public F decodeField(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
        return this.defaultValueFactory != null
                ? struct.field(this.name, ctx, this.endec, this.defaultValueFactory.get())
                : struct.field(this.name, ctx, this.endec);
    }

    public static final class Flat<S, F> extends StructField<S, F> {

        public Flat(StructEndec<F> endec, Function<S, F> getter) {
            super("", endec, getter, (Supplier<F>) null);
        }

        private StructEndec<F> endec() {
            return (StructEndec<F>) this.endec;
        }

        @Override
        public void encodeField(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, S instance) {
            this.endec().encodeStruct(ctx, serializer, struct, this.getter.apply(instance));
        }

        @Override
        public F decodeField(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
            return this.endec().decodeStruct(ctx, deserializer, struct);
        }
    }
}
