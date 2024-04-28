package io.wispforest.endec.util;

public interface QuintaConsumer<A, B, C, D, E> {
    void accept(A a, B b, C c, D d, E e);
}