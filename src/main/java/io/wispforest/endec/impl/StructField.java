package io.wispforest.endec.impl;


import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.data.ExtraDataContext;
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

    public void encodeField(Serializer<?> serializer, Serializer.Struct struct, ExtraDataContext ctx, S instance) {
        struct.field(ctx, this.name, this.endec, this.getter.apply(instance));
    }

    public F decodeField(Deserializer<?> deserializer, Deserializer.Struct struct, ExtraDataContext ctx) {
        return this.defaultValueFactory != null
                ? struct.field(ctx, this.name, this.endec, this.defaultValueFactory.get())
                : struct.field(ctx, this.name, this.endec);
    }

    public static final class Flat<S, F> extends StructField<S, F> {

        public Flat(StructEndec<F> endec, Function<S, F> getter) {
            super("", endec, getter, (Supplier<F>) null);
        }

        private StructEndec<F> endec() {
            return (StructEndec<F>) this.endec;
        }

        @Override
        public void encodeField(Serializer<?> serializer, Serializer.Struct struct, ExtraDataContext ctx, S instance) {
            this.endec().encodeStruct(serializer, struct, ctx, this.getter.apply(instance));
        }

        @Override
        public F decodeField(Deserializer<?> deserializer, Deserializer.Struct struct, ExtraDataContext ctx) {
            return this.endec().decodeStruct(deserializer, struct, ctx);
        }
    }
}
