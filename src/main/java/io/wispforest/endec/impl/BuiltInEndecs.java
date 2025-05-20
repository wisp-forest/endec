package io.wispforest.endec.impl;

import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationAttributes;

import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public final class BuiltInEndecs{

    private BuiltInEndecs() {}

    // --- Java Types ---

    public static final Endec<int[]> INT_ARRAY = Endec.INT.listOf().xmap((list) -> list.stream().mapToInt(v -> v).toArray(), (ints) -> Arrays.stream(ints).boxed().toList());
    public static final Endec<long[]> LONG_ARRAY = Endec.LONG.listOf().xmap((list) -> list.stream().mapToLong(v -> v).toArray(), (longs) -> Arrays.stream(longs).boxed().toList());

    public static final Endec<BitSet> BITSET = LONG_ARRAY.xmap(BitSet::valueOf, BitSet::toLongArray);

    public static final Endec<java.util.UUID> UUID = Endec
            .ifAttr(
                    SerializationAttributes.HUMAN_READABLE,
                    Endec.STRING.xmap(java.util.UUID::fromString, java.util.UUID::toString)
            ).orElse(
                    INT_ARRAY.xmap(BuiltInEndecs::toUuid, BuiltInEndecs::toIntArray)
            );

    public static final Endec<Date> DATE = Endec
            .ifAttr(
                    SerializationAttributes.HUMAN_READABLE,
                    Endec.STRING.xmap(s -> Date.from(Instant.parse(s)), date -> date.toInstant().toString())
            ).orElse(
                    Endec.LONG.xmap(Date::new, Date::getTime)
            );

    private static java.util.UUID toUuid(int[] array) {
        return new UUID((long)array[0] << 32 | (long)array[1] & 4294967295L, (long)array[2] << 32 | (long)array[3] & 4294967295L);
    }

    private static int[] toIntArray(UUID uuid) {
        return toIntArray(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    private static int[] toIntArray(long uuidMost, long uuidLeast) {
        return new int[]{(int)(uuidMost >> 32), (int)uuidMost, (int)(uuidLeast >> 32), (int)uuidLeast};
    }

    public static <C, V> Endec<V> vectorEndec(String name, Endec<C> componentEndec, BiFunction<C, C, V> constructor, Function<V, C> xGetter, Function<V, C> yGetter) {
        return componentEndec.listOf()
                .validate(validateSize(name, 2))
                .xmap(
                        components -> constructor.apply(components.get(0), components.get(1)),
                        vector -> List.of(xGetter.apply(vector), yGetter.apply(vector))
                );
    }

    public static <C, V> Endec<V> vectorEndec(String name, Endec<C> componentEndec, StructEndecBuilder.Function3<C, C, C, V> constructor, Function<V, C> xGetter, Function<V, C> yGetter, Function<V, C> zGetter) {
        return componentEndec.listOf()
                .validate(validateSize(name, 3))
                .xmap(
                        components -> constructor.apply(components.get(0), components.get(1), components.get(2)),
                        vector -> List.of(xGetter.apply(vector), yGetter.apply(vector), zGetter.apply(vector))
                );
    }

    public static <C, V> Endec<V> vectorEndec(String name, Endec<C> componentEndec, StructEndecBuilder.Function4<C, C, C, C, V> constructor, Function<V, C> xGetter, Function<V, C> yGetter, Function<V, C> zGetter, Function<V, C> wGetter) {
        return componentEndec.listOf()
                .validate(validateSize(name, 4))
                .xmap(
                        components -> constructor.apply(components.get(0), components.get(1), components.get(2), components.get(3)),
                        vector -> List.of(xGetter.apply(vector), yGetter.apply(vector), zGetter.apply(vector), wGetter.apply(vector))
                );
    }

    private static <C> Consumer<List<C>> validateSize(String name, int requiredSize) {
        return collection -> { if (collection.size() != 4) throw new IllegalStateException(name + "collection must have " + requiredSize + " elements"); };
    }
}
