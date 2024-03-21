package io.wispforest.endec.impl;

import io.wispforest.endec.*;
import io.wispforest.endec.util.QuadConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class StructEndecBranchedBuilder<T> {

    private final Map<DataToken<?>, OptionalEndec<T>> branches = new LinkedHashMap<>();

    private StructEndecBranchedBuilder(){}

    public static <T, C> StructEndecBranchedBuilder<T> of(DataToken<C> token, QuadConsumer<Serializer<?>, Serializer.Struct, C, T> encode, StructEndecBuilder.Function3<Deserializer<?>, Deserializer.Struct, C, T> decode){
        return new StructEndecBranchedBuilder<T>().orElseIf(token, encode, decode);
    }

    public static <T, I, D> StructEndecBranchedBuilder<T> of(DataToken<D> attribute, StructEndec<I> endec, BiFunction<D, T, I> to, BiFunction<D, I, T> from) {
        return StructEndecBranchedBuilder.of(attribute, (serializer, struct, d, t) -> endec.encodeStruct(serializer, struct, to.apply(d, t)), (deserializer, struct, d) -> from.apply(d, endec.decodeStruct(deserializer, struct)));
    }

    public static <T> StructEndecBranchedBuilder<T> of(DataToken<?> token, StructEndec<T> endec){
        return new StructEndecBranchedBuilder<T>().orElseIf(token, endec);
    }

    public <C> StructEndecBranchedBuilder<T> orElseIf(DataToken<C> token, QuadConsumer<Serializer<?>, Serializer.Struct, C, T> encode, StructEndecBuilder.Function3<Deserializer<?>, Deserializer.Struct, C, T> decode) {
        if(this.branches.containsKey(token)) {
            throw new IllegalStateException("Unable to add a branch for the given Endec due to already containing such in the map! [Name: " + token.name() + "]");
        }

        this.branches.put(token, new OptionalEndec<>() {
            @Override
            public boolean encode(Serializer<?> serializer, Serializer.Struct struct, T value) {
                if (!serializer.has(token)) return false;

                encode.accept(serializer, struct, serializer.get(token), value);

                return true;
            }

            @Override
            @Nullable
            public T decode(Deserializer<?> deserializer, Deserializer.Struct struct) {
                if (!deserializer.has(token)) return null;

                return decode.apply(deserializer, struct, deserializer.get(token));
            }
        });

        return this;
    }

    public StructEndecBranchedBuilder<T> orElseIf(DataToken<?> token, StructEndec<T> endec) {
        if(this.branches.containsKey(token)) {
            throw new IllegalStateException("Unable to add a branch for the given Endec due to already containing such in the map! [Name: " + token.name() + "]");
        }

        this.branches.put(token, new OptionalEndec<T>() {
            @Override
            public boolean encode(Serializer<?> serializer, Serializer.Struct struct, T value) {
                if(!serializer.has(token)) return false;

                endec.encodeStruct(serializer, struct, value);

                return true;
            }

            @Override
            @Nullable
            public T decode(Deserializer<?> deserializer, Deserializer.Struct struct) {
                if(!deserializer.has(token)) return null;

                return endec.decodeStruct(deserializer, struct);
            }
        });

        return this;
    }

    public Endec<T> orElse(Endec<T> endec){
        return new StructEndec<T>() {
            @Override
            public void encodeStruct(Serializer<?> serializer, Serializer.Struct struct, T value) {
                for (var entry : branches.values()) {
                    if (entry.encode(serializer, struct, value)) return;
                }

                endec.encode(serializer, value);
            }

            @Override
            public T decodeStruct(Deserializer<?> deserializer, Deserializer.Struct struct) {
                for (var entry : branches.values()) {
                    var t = entry.decode(deserializer, struct);

                    if (t != null) return t;
                }

                return endec.decode(deserializer);
            }
        };
    }

    private interface OptionalEndec<T> {
        boolean encode(Serializer<?> serializer, Serializer.Struct struct, T value);

        @Nullable T decode(Deserializer<?> deserializer, Deserializer.Struct struct);
    }
}
