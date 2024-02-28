package io.wispforest.endec.impl;


import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.Serializer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public final class StructField<S, F> {

    private final String name;
    private final Endec<F> endec;
    private final Function<S, F> getter;
    private final @Nullable Supplier<F> defaultValueFactory;

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

    public void encodeField(Serializer.Struct struct, S instance) {
        struct.field(this.name, this.endec, this.getter.apply(instance));
    }

    public F decodeField(Deserializer.Struct struct) {
        return this.defaultValueFactory != null
                ? struct.field(this.name, this.endec, this.defaultValueFactory.get())
                : struct.field(this.name, this.endec);
    }
}
