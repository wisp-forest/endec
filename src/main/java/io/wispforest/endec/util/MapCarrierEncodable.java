package io.wispforest.endec.util;

import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.impl.KeyedEndec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MapCarrierEncodable {

    /**
     * Store {@code value} under {@code key} in this object's associated map
     */
    default <T> void put(SerializationContext ctx, @NotNull KeyedEndec<T> key, @NotNull T value) {
        throw new UnsupportedOperationException("Interface default method called");
    }

    default <T> void put(@NotNull KeyedEndec<T> key, @NotNull T value) {
        put(SerializationContext.empty(), key, value);
    }

    /**
     * Delete the value stored under {@code key} from this object's associated map,
     * if it is present
     */
    default <T> void delete(@NotNull KeyedEndec<T> key) {
        throw new UnsupportedOperationException("Interface default method called");
    }

    default <T> void putIfNotNull(@NotNull KeyedEndec<T> key, @Nullable T value) {
        putIfNotNull(SerializationContext.empty(), key, value);
    }

    /**
     * If {@code value} is not {@code null}, store it under {@code key} in this
     * object's associated map
     */
    default <T> void putIfNotNull(SerializationContext ctx, @NotNull KeyedEndec<T> key, @Nullable T value) {
        if (value == null) return;
        this.put(ctx, key, value);
    }
}
