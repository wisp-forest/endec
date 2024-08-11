package io.wispforest.endec.util;

import io.wispforest.endec.SerializationContext;
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
    default <T> T getWithErrors(SerializationContext ctx, @NotNull KeyedEndec<T> key) {
        throw new UnsupportedOperationException("Interface default method called");
    }

    default <T> T getWithErrors(@NotNull KeyedEndec<T> key) {
        return getWithErrors(SerializationContext.empty(), key);
    }

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

    default <T> void copy(@NotNull KeyedEndec<T> key, @NotNull MapCarrier other) {
        copy(SerializationContext.empty(), key, other);
    }

    /**
     * Store the value associated with {@code key} in this object's associated map
     * into the associated map of {@code other} under {@code key}
     * <p>
     * Importantly, this does not copy the value itself - be careful with mutable types
     */
    default <T> void copy(SerializationContext ctx, @NotNull KeyedEndec<T> key, @NotNull MapCarrier other) {
        other.put(ctx, key, this.get(ctx, key));
    }

    default <T> void copyIfPresent(@NotNull KeyedEndec<T> key, @NotNull MapCarrier other) {
        copyIfPresent(SerializationContext.empty(), key, other);
    }

    /**
     * Like {@link #copy(SerializationContext, KeyedEndec, MapCarrier)}, but only if this object's associated map
     * has a value stored under {@code key}
     */
    default <T> void copyIfPresent(SerializationContext ctx, @NotNull KeyedEndec<T> key, @NotNull MapCarrier other) {
        if (!this.has(key)) return;
        this.copy(ctx, key, other);
    }

    default <T> void mutate(@NotNull KeyedEndec<T> key, @NotNull Function<T, T> mutator) {
        mutate(SerializationContext.empty(), key, mutator);
    }

    /**
     * Get the value stored under {@code key} in this object's associated map, apply
     * {@code mutator} to it and store the result under {@code key}
     */
    default <T> void mutate(SerializationContext ctx, @NotNull KeyedEndec<T> key, @NotNull Function<T, T> mutator) {
        this.put(ctx, key, mutator.apply(this.get(ctx, key)));
    }

    //--

    /**
     * @deprecated Use {@link #copyIfPresent(SerializationContext, KeyedEndec, MapCarrier)} instead!
     */
    @Deprecated(forRemoval = true)
    default <T> void copyIfPresent(@NotNull KeyedEndec<T> key, SerializationContext ctx, @NotNull MapCarrier other) {
        copyIfPresent(ctx, key, other);
    }

    /**
     * @deprecated Use {@link #mutate(SerializationContext, KeyedEndec, Function)} instead!
     */
    @Deprecated(forRemoval = true)
    default <T> void mutate(@NotNull KeyedEndec<T> key, SerializationContext ctx, @NotNull Function<T, T> mutator) {
        mutate(ctx, key, mutator);
    }
}
