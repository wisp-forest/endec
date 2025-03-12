package io.wispforest.endec.struct.inheritence;

public record ImmutablePairTest<K, V>(K left, V right) implements it.unimi.dsi.fastutil.Pair<K, V> {
}
