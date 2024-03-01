package io.wispforest.endec.util;

public interface TriConsumer<A, B, C> {
    void accept(A a, B b, C c);
}
