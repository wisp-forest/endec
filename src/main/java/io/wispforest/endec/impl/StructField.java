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
        try {
            struct.field(this.name, ctx, this.endec, this.getter.apply(instance));
        } catch (StructFieldException e) {
            throw e;
        } catch (Exception e) {
            throw StructFieldException.of(name, e, true);
        }
    }

    public F decodeField(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
        try {
            return this.defaultValueFactory != null
                    ? struct.field(this.name, ctx, this.endec, this.defaultValueFactory.get())
                    : struct.field(this.name, ctx, this.endec);
        } catch (StructFieldException e) {
            throw e;
        } catch (Exception e) {
            throw StructFieldException.of(this.name, e, false);
        }
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
            try {
                this.endec().encodeStruct(ctx, serializer, struct, this.getter.apply(instance));
            } catch (StructFieldException e) {
                throw e;
            } catch (Exception e) {
                throw StructFieldException.of(name, e, true);
            }
        }

        @Override
        public F decodeField(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
            try {
                return this.endec().decodeStruct(ctx, deserializer, struct);
            } catch (StructFieldException e) {
                throw e;
            } catch (Exception e) {
                throw StructFieldException.of(name, e, true);
            }
        }
    }

}
