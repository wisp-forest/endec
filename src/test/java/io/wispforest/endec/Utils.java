package io.wispforest.endec;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Utils {

    public static <T> T make(Supplier<T> supplier, Consumer<T> consumer) {
        var t = supplier.get();

        consumer.accept(t);

        return t;
    }
}
