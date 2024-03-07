package io.wispforest.endec;

import java.util.function.BiConsumer;

public record DataTokenHolder<DATA_TYPE>(DataToken<DATA_TYPE> token, DATA_TYPE data) {

    public void consume(BiConsumer<DataToken<DATA_TYPE>, DATA_TYPE> consumer){
        consumer.accept(token(), data());
    }
}
