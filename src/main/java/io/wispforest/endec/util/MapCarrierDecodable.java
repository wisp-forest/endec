package io.wispforest.endec.util;

import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.impl.KeyedEndec;
import org.jetbrains.annotations.NotNull;

public interface MapCarrierDecodable {

    /**
     * Get the value stored under {@code key} in this object's associated map.
     * If no such value exists, the default value of {@code key} is returned
     * <p>
     * Any exceptions thrown during decoding are propagated to the caller
     */
    default <T> T getWithErrors(SerializationContext ctx, @NotNull KeyedEndec<T> key) {
        throw new UnsupportedOperationException("Interface default method called");
    }

    default <T> T getWithErrors(@NotNull KeyedEndec<T> key) {
        return getWithErrors(SerializationContext.empty(), key);
    }

    /**
     * Get the value stored under {@code key} in this object's associated map.
     * If no such value exists <i>or</i> an exception is thrown during decoding,
     * the default value of {@code key} is returned
     */
    default <T> T get(SerializationContext ctx, @NotNull KeyedEndec<T> key) {
        try {
            return this.getWithErrors(ctx, key);
        } catch (Exception e) {
            return key.defaultValue();
        }
    }

    default <T> T get(@NotNull KeyedEndec<T> key) {
        return get(SerializationContext.empty(), key);
    }

    /**
     * Test whether there is a value stored under {@code key} in this object's associated map
     */
    default <T> boolean has(@NotNull KeyedEndec<T> key) {
        throw new UnsupportedOperationException("Interface default method called");
    }

    //--

    default <T> void copy(@NotNull KeyedEndec<T> key, @NotNull MapCarrierEncodable other) {
        copy(SerializationContext.empty(), key, other);
    }

    /**
     * Store the value associated with {@code key} in this object's associated map
     * into the associated map of {@code other} under {@code key}
     * <p>
     * Importantly, this does not copy the value itself - be careful with mutable types
     */
    default <T> void copy(SerializationContext ctx, @NotNull KeyedEndec<T> key, @NotNull MapCarrierEncodable other) {
        other.put(ctx, key, this.get(ctx, key));
    }

    default <T> void copyIfPresent(@NotNull KeyedEndec<T> key, @NotNull MapCarrierEncodable other) {
        copyIfPresent(SerializationContext.empty(), key, other);
    }

    /**
     * Like {@link #copy(SerializationContext, KeyedEndec, MapCarrierEncodable)}, but only if this object's associated map
     * has a value stored under {@code key}
     */
    default <T> void copyIfPresent(SerializationContext ctx, @NotNull KeyedEndec<T> key, @NotNull MapCarrierEncodable other) {
        if (!this.has(key)) return;
        this.copy(ctx, key, other);
    }

    //--

    /**
     * @deprecated Use {@link #copyIfPresent(SerializationContext, KeyedEndec, MapCarrierEncodable)} instead!
     */
    @Deprecated(forRemoval = true)
    default <T> void copyIfPresent(@NotNull KeyedEndec<T> key, SerializationContext ctx, @NotNull MapCarrier other) {
        copyIfPresent(ctx, key, other);
    }
}
