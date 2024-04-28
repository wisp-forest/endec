package io.wispforest.endec.util;

import io.wispforest.endec.data.DataToken;
import io.wispforest.endec.data.ExtraDataContext;
import io.wispforest.endec.impl.KeyedEndec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface MapCarrier {

    /**
     * Get the value stored under {@code key} in this object's associated map.
     * If no such value exists, the default value of {@code key} is returned
     * <p>
     * Any exceptions thrown during decoding are propagated to the caller
     */
    default <T> T getWithErrors(@NotNull KeyedEndec<T> key, ExtraDataContext ctx) {
        throw new UnsupportedOperationException("Interface default method called");
    }

    default <T> T getWithErrors(@NotNull KeyedEndec<T> key, DataToken.Instance ...instances) {
        return getWithErrors(key, ExtraDataContext.of(instances));
    }

    /**
     * Store {@code value} under {@code key} in this object's associated map
     */
    default <T> void put(@NotNull KeyedEndec<T> key, ExtraDataContext ctx, @NotNull T value) {
        throw new UnsupportedOperationException("Interface default method called");
    }

    default <T> void put(@NotNull KeyedEndec<T> key, @NotNull T value, DataToken.Instance ...instances) {
        put(key, ExtraDataContext.of(instances), value);
    }

    /**
     * Delete the value stored under {@code key} from this object's associated map,
     * if it is present
     */
    default <T> void delete(@NotNull KeyedEndec<T> key) {
        throw new UnsupportedOperationException("Interface default method called");
    }

    /**
     * Test whether there is a value stored under {@code key} in this object's associated map
     */
    default <T> boolean has(@NotNull KeyedEndec<T> key) {
        throw new UnsupportedOperationException("Interface default method called");
    }

    // ---

    /**
     * Get the value stored under {@code key} in this object's associated map.
     * If no such value exists <i>or</i> an exception is thrown during decoding,
     * the default value of {@code key} is returned
     */
    default <T> T get(@NotNull KeyedEndec<T> key, ExtraDataContext ctx) {
        try {
            return this.getWithErrors(key, ctx);
        } catch (Exception e) {
            return key.defaultValue();
        }
    }

    default <T> T get(@NotNull KeyedEndec<T> key, DataToken.Instance ...instances) {
        return get(key, ExtraDataContext.of(instances));
    }

    /**
     * If {@code value} is not {@code null}, store it under {@code key} in this
     * object's associated map
     */
    default <T> void putIfNotNull(@NotNull KeyedEndec<T> key, ExtraDataContext ctx, @Nullable T value) {
        if (value == null) return;
        this.put(key, ctx, value);
    }

    /**
     * Store the value associated with {@code key} in this object's associated map
     * into the associated map of {@code other} under {@code key}
     * <p>
     * Importantly, this does not copy the value itself - be careful with mutable types
     */
    default <T> void copy(@NotNull KeyedEndec<T> key, ExtraDataContext ctx, @NotNull MapCarrier other) {
        other.put(key, ctx, this.get(key, ctx));
    }

    /**
     * Like {@link #copy(KeyedEndec, ExtraDataContext, MapCarrier)}, but only if this object's associated map
     * has a value stored under {@code key}
     */
    default <T> void copyIfPresent(@NotNull KeyedEndec<T> key, ExtraDataContext ctx, @NotNull MapCarrier other) {
        if (!this.has(key)) return;
        this.copy(key, ctx, other);
    }

    /**
     * Get the value stored under {@code key} in this object's associated map, apply
     * {@code mutator} to it and store the result under {@code key}
     */
    default <T> void mutate(@NotNull KeyedEndec<T> key, ExtraDataContext ctx, @NotNull Function<T, T> mutator) {
        this.put(key, ctx, mutator.apply(this.get(key, ctx)));
    }
}
