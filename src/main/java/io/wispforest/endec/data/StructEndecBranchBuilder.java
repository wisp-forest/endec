package io.wispforest.endec.data;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.util.QuadFunction;
import io.wispforest.endec.util.QuintaConsumer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class StructEndecBranchBuilder<T> {

    private final Map<DataToken<?>, ConditionalEndec<T, StructEndec<T>>> branches = new LinkedHashMap<>();

    public StructEndecBranchBuilder(){}

    public <I, C> StructEndecBranchBuilder<T> orElseIf(DataToken.Instanced<C> token, StructEndec<I> endec, BiFunction<C, I, T> to, BiFunction<C, T, I> from) {
        return this.orElseIf(token, (serializer, struct, ctx, c, t) -> endec.encodeStruct(serializer, struct, ctx, from.apply(c, t)), (deserializer, struct, ctx, c) -> to.apply(c, endec.decodeStruct(deserializer, struct, ctx)));
    }

    public <C> StructEndecBranchBuilder<T> orElseIf(DataToken.Instanced<C> token, QuintaConsumer<Serializer<?>, Serializer.Struct, ExtraDataContext, C, T> encode, QuadFunction<Deserializer<?>, Deserializer.Struct, ExtraDataContext, C, T> decode) {
        return this.orElseIf(token, StructEndec.ofTokenStruct(token, encode, decode));
    }

    public StructEndecBranchBuilder<T> orElseIf(DataToken<?> token, StructEndec<T> endec) {
        if(this.branches.containsKey(token)) {
            throw new IllegalStateException("Unable to add a branch for the given Endec due to already containing such in the map! [Name: " + token.name() + "]");
        }

        var conditionalEndec = (token.clazz.equals(Boolean.class))
                    ? ConditionalEndec.ofBl((DataToken.Instanced<Boolean>) token, endec)
                    : ConditionalEndec.of(token, endec);

        this.branches.put(token, conditionalEndec);

        return this;
    }

    public StructEndec<T> orElse(StructEndec<T> endec){
        if(branches.size() < 1) {
            throw new IllegalStateException("A given branched Endec was attempted to be made but was found to either have one or no branches making it pointless!");
        }

        return new StructEndec<T>() {
            @Override
            public void encodeStruct(Serializer<?> serializer, Serializer.Struct struct, ExtraDataContext ctx, T value) {
                var selectedEndec = endec;

                for (var entry : branches.values()) {
                    if (!entry.useForEncode(serializer, ctx)) continue;

                    selectedEndec = entry.endec();

                    break;
                }

                selectedEndec.encodeStruct(serializer, struct, ctx, value);
            }

            @Override
            public T decodeStruct(Deserializer<?> deserializer, Deserializer.Struct struct,  ExtraDataContext ctx) {
                var selectedEndec = endec;

                for (var entry : branches.values()) {
                    if(!entry.useForDecode(deserializer, ctx)) continue;

                    selectedEndec = entry.endec();

                    break;
                }

                return selectedEndec.decodeStruct(deserializer, struct, ctx);
            }
        };
    }

}
