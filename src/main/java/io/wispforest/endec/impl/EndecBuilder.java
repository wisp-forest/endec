package io.wispforest.endec.impl;

import io.wispforest.endec.DataToken;
import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class EndecBuilder<T> {

    private final Map<DataToken<?>, OptionalEndec<T>> branches = new LinkedHashMap<>();

    private EndecBuilder(){}

    public static <T, C> EndecBuilder<T> of(DataToken<C> token, TriConsumer<Serializer<?>, C, T> encode, BiFunction<Deserializer<?>, C, T> decode){
        return new EndecBuilder<T>().orElseIf(token, encode, decode);
    }

    public static <T> EndecBuilder<T> of(DataToken<?> token, Endec<T> endec){
        return new EndecBuilder<T>().orElseIf(token, endec);
    }

    public <C> EndecBuilder<T> orElseIf(DataToken<C> token, TriConsumer<Serializer<?>, C, T> encode, BiFunction<Deserializer<?>, C, T> decode) {
        if(this.branches.containsKey(token)) {
            throw new IllegalStateException("Unable to add a branch for the given Endec due to already containing such in the map! [Name: " + token.name() + "]");
        }

        this.branches.put(token, new OptionalEndec<>() {
            @Override
            public boolean encode(Serializer<?> serializer, T value) {
                if (!serializer.has(token)) return false;

                encode.accept(serializer, serializer.get(token), value);

                return true;
            }

            @Override
            @Nullable
            public T decode(Deserializer<?> deserializer) {
                if (!deserializer.has(token)) return null;

                return decode.apply(deserializer, deserializer.get(token));
            }
        });

        return this;
    }

    public EndecBuilder<T> orElseIf(DataToken<?> token, Endec<T> endec) {
        if(this.branches.containsKey(token)) {
            throw new IllegalStateException("Unable to add a branch for the given Endec due to already containing such in the map! [Name: " + token.name() + "]");
        }

        this.branches.put(token, new OptionalEndec<T>() {
            @Override
            public boolean encode(Serializer<?> serializer, T value) {
                if(!serializer.has(token)) return false;

                endec.encode(serializer, value);

                return true;
            }

            @Override
            @Nullable
            public T decode(Deserializer<?> deserializer) {
                if(!deserializer.has(token)) return null;

                return endec.decode(deserializer);
            }
        });

        return this;
    }

    public Endec<T> orElse(Endec<T> endec){
        return new Endec<>() {
            @Override
            public void encode(Serializer<?> serializer, T value) {
                for (var entry : branches.values()) {
                    if (entry.encode(serializer, value)) return;
                }

                endec.encode(serializer, value);
            }

            @Override
            public T decode(Deserializer<?> deserializer) {
                for (var entry : branches.values()) {
                    var t = entry.decode(deserializer);

                    if (t != null) return t;
                }

                return endec.decode(deserializer);
            }
        };
    }

    private interface OptionalEndec<T> {
        boolean encode(Serializer<?> serializer, T value);

        @Nullable T decode(Deserializer<?> deserializer);
    }
}
