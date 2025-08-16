package io.wispforest.endec.util;

import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.impl.KeyedEndec;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public interface MapCarrier extends MapCarrierDecodable, MapCarrierEncodable {

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
     * @deprecated Use {@link #mutate(SerializationContext, KeyedEndec, Function)} instead!
     */
    @Deprecated(forRemoval = true)
    default <T> void mutate(@NotNull KeyedEndec<T> key, SerializationContext ctx, @NotNull Function<T, T> mutator) {
        mutate(ctx, key, mutator);
    }
}
