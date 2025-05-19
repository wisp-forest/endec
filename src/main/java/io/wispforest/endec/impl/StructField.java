package io.wispforest.endec.impl;

import io.wispforest.endec.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed class StructField<S, F> permits StructField.Flat, StructField.MutableField {

    protected final String name;
    protected final Endec<F> endec;
    protected final Function<S, F> getter;
    protected final @Nullable Supplier<F> defaultValueFactory;
    protected final SerializationContext context;

    @ApiStatus.Internal
    public StructField(String name, Endec<F> endec, Function<S, F> getter, @Nullable Supplier<F> defaultValueFactory, SerializationContext context) {
        this.name = name;
        this.endec = endec;
        this.getter = getter;
        this.defaultValueFactory = defaultValueFactory;
        this.context = context;
    }

    @ApiStatus.Internal
    public StructField(String name, Endec<F> endec, Function<S, F> getter, @Nullable Supplier<F> defaultValueFactory) {
        this(name, endec, getter, defaultValueFactory, SerializationContext.empty());
    }

    @ApiStatus.Internal
    public StructField(String name, Endec<F> endec, Function<S, F> getter, @Nullable F defaultValue) {
        this(name, endec, getter, () -> defaultValue);
    }

    @ApiStatus.Internal
    public StructField(String name, Endec<F> endec, Function<S, F> getter) {
        this(name, endec, getter, (Supplier<F>) null);
    }

    public StructField<S, F> withContext(SerializationContext context) {
        return new StructField<>(this.name, this.endec, this.getter, this.defaultValueFactory, this.context.and(context));
    }

    public void encodeField(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, S instance) {
        try {
            struct.field(this.name, ctx.and(this.context), this.endec, this.getter.apply(instance), this.defaultValueFactory != null);
        } catch (Exception e) {
            throw new StructFieldException("Exception occurred when encoding a given StructField: [Field: " + this.name + "]", e);
        }
    }

    public F decodeField(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
        try {
            return struct.field(this.name, ctx.and(this.context), this.endec, this.defaultValueFactory);
        } catch (Exception e) {
            throw new StructFieldException("Exception occurred when decoding a given StructField: [Field: " + this.name + "]", e);
        }
    }

    public static final class Flat<S, F> extends StructField<S, F> {

        @ApiStatus.Internal
        public Flat(StructEndec<F> endec, Function<S, F> getter, SerializationContext context) {
            super("", endec, getter, (Supplier<F>) null, context);
        }

        public Flat(StructEndec<F> endec, Function<S, F> getter) {
            this(endec, getter, SerializationContext.empty());
        }

        private StructEndec<F> endec() {
            return (StructEndec<F>) this.endec;
        }

        @Override
        public Flat<S, F> withContext(SerializationContext context) {
            return new Flat<S, F>((StructEndec<F>) this.endec, this.getter, this.context.and(context));
        }

        @Override
        public void encodeField(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, S instance) {
            this.endec().encodeStruct(ctx.and(this.context), serializer, struct, this.getter.apply(instance));
        }

        @Override
        public F decodeField(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
            return this.endec().decodeStruct(ctx.and(this.context), deserializer, struct);
        }
    }

    public static class StructFieldException extends IllegalStateException {
        public StructFieldException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final class MutableField<S, F> extends StructField<S, F> {

        private final BiConsumer<S, F> setter;

        public MutableField(String name, Endec<F> endec, Function<S, F> getter, BiConsumer<S, F> setter, SerializationContext context) {
            super(name, endec, getter, (Supplier<F>) null, context);

            this.setter = setter;
        }

        public MutableField(String name, Endec<F> endec, Function<S, F> getter, BiConsumer<S, F> setter) {
            this(name, endec, getter, setter, SerializationContext.empty());
        }

        @Override
        public MutableField<S, F> withContext(SerializationContext context) {
            return new MutableField<S, F>(this.name, this.endec, this.getter, this.setter, this.context.and(context));
        }

        public void decodeField(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct, S s) {
            try {
                F f = struct.field(this.name, ctx.and(this.context), this.endec, this.defaultValueFactory);

                setter.accept(s, f);
            } catch (Exception e) {
                throw new StructFieldException("Exception occurred when decoding a given StructField: [Field: " + this.name + "]", e);
            }
        }
    }
}
