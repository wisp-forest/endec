package io.wispforest.endec.data;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.util.ContextedStructDecoder;
import io.wispforest.endec.util.ContextedStructEncoder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class StructEndecBranchBuilder<T> {

    private final Map<DataToken<?>, ConditionalEndec<T, StructEndec<T>>> branches = new LinkedHashMap<>();

    public StructEndecBranchBuilder(){}

    public <I, C> StructEndecBranchBuilder<T> orElseIf(DataToken.Instanced<C> token, StructEndec<I> endec, BiFunction<C, I, T> to, BiFunction<C, T, I> from) {
        return this.orElseIf(token, (ctx, serializer, struct, c, t) -> endec.encodeStruct(ctx, serializer, struct, from.apply(c, t)), (ctx, deserializer, struct, c) -> to.apply(c, endec.decodeStruct(ctx, deserializer, struct)));
    }

    public <C> StructEndecBranchBuilder<T> orElseIf(DataToken.Instanced<C> token, ContextedStructEncoder<T, C> encode, ContextedStructDecoder<T, C> decode) {
        return this.orElseIf(token, StructEndec.ofTokenStruct(token, encode, decode));
    }

    public StructEndecBranchBuilder<T> orElseIf(DataToken<?> token, StructEndec<T> endec) {
        if(this.branches.containsKey(token)) {
            throw new IllegalStateException("Unable to add a branch for the given Endec due to already containing such in the map! [Name: " + token.name() + "]");
        }

        var conditionalEndec = (token.clazz().equals(Boolean.class))
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
            public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, T value) {
                var selectedEndec = endec;

                for (var entry : branches.values()) {
                    if (!entry.useForEncode(serializer, ctx)) continue;

                    selectedEndec = entry.endec();

                    break;
                }

                selectedEndec.encodeStruct(ctx, serializer, struct, value);
            }

            @Override
            public T decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
                var selectedEndec = endec;

                for (var entry : branches.values()) {
                    if(!entry.useForDecode(deserializer, ctx)) continue;

                    selectedEndec = entry.endec();

                    break;
                }

                return selectedEndec.decodeStruct(ctx, deserializer, struct);
            }
        };
    }

}
