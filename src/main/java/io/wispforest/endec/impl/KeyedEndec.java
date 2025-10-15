package io.wispforest.endec.impl;

import io.wispforest.endec.Endec;

import java.util.function.Supplier;

public final class KeyedEndec<F> {

    private final String key;
    private final Endec<F> endec;
    private final Supplier<F> defaultValueFactory;

    public KeyedEndec(String key, Endec<F> endec, Supplier<F> defaultValueFactory) {
        this.key = key;
        this.endec = endec;
        this.defaultValueFactory = defaultValueFactory;
    }

    public KeyedEndec(String key, Endec<F> endec, F defaultValue) {
        this(key, endec, () -> defaultValue);
    }

    public String key() {
        return this.key;
    }

    public Endec<F> endec() {
        return this.endec;
    }

    public F defaultValue() {
        return this.defaultValueFactory.get();
    }

    public Supplier<F> defaultValueFactory() {
        return this.defaultValueFactory;
    }

    @Override
    public String toString() {
        return "KeyedEndec[Key: '" + key + "', Endec: " + endec + "]";
    }
}
