package io.wispforest.endec.util;

import org.jetbrains.annotations.Nullable;

public class MutableHolder<O> {
    @Nullable
    private O value = null;

    public MutableHolder<O> setValue(O value) {
        this.value = value;

        return this;
    }

    public O getValue() {
        return this.value;
    }
}
