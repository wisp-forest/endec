package io.wispforest.endec.data;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.util.QuadConsumer;
import io.wispforest.endec.util.TriFunction;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class EndecBranchBuilder<T> {

    private final Map<DataToken<?>, ConditionalEndec<T, Endec<T>>> branches = new LinkedHashMap<>();

    public EndecBranchBuilder(){}

    public <I, C> EndecBranchBuilder<T> orElseIf(DataToken.Instanced<C> token, Endec<I> endec, BiFunction<C, I, T> to, BiFunction<C, T, I> from) {
        return this.orElseIf(token, (serializer, ctx, c, t) -> endec.encode(serializer, ctx, from.apply(c, t)), (deserializer, ctx, c) -> to.apply(c, endec.decode(deserializer, ctx)));
    }

    public <C> EndecBranchBuilder<T> orElseIf(DataToken.Instanced<C> token, QuadConsumer<Serializer<?>, ExtraDataContext, C, T> encode, TriFunction<Deserializer<?>, ExtraDataContext, C, T> decode) {
        return this.orElseIf(token, Endec.ofToken(token, encode, decode));
    }

    public EndecBranchBuilder<T> orElseIf(DataToken<?> token, Endec<T> endec) {
        if(this.branches.containsKey(token)) {
            throw new IllegalStateException("Unable to add a branch for the given Endec due to already containing such in the map! [Name: " + token.name() + "]");
        }

        var conditionalEndec = (token.clazz.equals(Boolean.class))
                    ? ConditionalEndec.ofBl((DataToken.Instanced<Boolean>) token, endec)
                    : ConditionalEndec.of(token, endec);

        this.branches.put(token, conditionalEndec);

        return this;
    }

    public Endec<T> orElse(Endec<T> endec){
        if(branches.size() < 1) {
            throw new IllegalStateException("A given branched Endec was attempted to be made but was found to either have one or no branches making it pointless!");
        }

        return new Endec<>() {
            @Override
            public void encode(Serializer<?> serializer, ExtraDataContext ctx, T value) {
                var selectedEndec = endec;

                for (var entry : branches.values()) {
                    if (!entry.useForEncode(serializer, ctx)) continue;

                    selectedEndec = entry.endec();

                    break;
                }

                selectedEndec.encode(serializer, ctx, value);
            }

            @Override
            public T decode(Deserializer<?> deserializer, ExtraDataContext ctx) {
                var selectedEndec = endec;

                for (var entry : branches.values()) {
                    if(!entry.useForDecode(deserializer, ctx)) continue;

                    selectedEndec = entry.endec();

                    break;
                }

                return selectedEndec.decode(deserializer, ctx);
            }
        };
    }

}
