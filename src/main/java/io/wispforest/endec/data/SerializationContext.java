package io.wispforest.endec.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.wispforest.endec.impl.MissingTokenDataException;
import io.wispforest.endec.impl.SuppressedTokenDataException;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public record SerializationContext(Map<DataToken<?>, Object> tokens, Set<DataToken<?>> suppressed) {

    public static SerializationContext of(DataToken.Instance... instances){
        return new SerializationContext(Map.of(), Set.of()).with(instances);
    }

    public static SerializationContext ofSuppressed(DataToken<?>... tokens){
        return new SerializationContext(Map.of(), Set.of()).withSuppressed(tokens);
    }

    public DataToken.Instance[] instances(){
        return DataToken.streamedData(tokens()).toArray(DataToken.Instance[]::new);
    }

    public SerializationContext with(DataToken.Instance... instances) {
        var map = new HashMap<>(tokens());

        for (var instance : instances) {
            map.put(instance.getToken(), instance.getValue());
        }

        return new SerializationContext(ImmutableMap.copyOf(map), ImmutableSet.copyOf(suppressed()));
    }

    public SerializationContext withSuppressed(DataToken<?>... tokens){
        var set = new HashSet<>(Arrays.asList(tokens));

        set.addAll(suppressed());

        return new SerializationContext(ImmutableMap.copyOf(tokens()), ImmutableSet.copyOf(set));
    }

    public SerializationContext withoutSuppressed(DataToken<?>... tokens){
        var set = new HashSet<>(suppressed());

        set.removeAll(Set.of(tokens));

        return new SerializationContext(ImmutableMap.copyOf(tokens()), ImmutableSet.copyOf(set));
    }

    public <DATA_TYPE> @Nullable DATA_TYPE get(DataToken<DATA_TYPE> token) {
        if(suppressed().contains(token)) return null;

        return (DATA_TYPE) this.tokens().get(token);
    }

    public <DATA_TYPE> DATA_TYPE getOrThrow(DataToken<DATA_TYPE> token){
        if(suppressed().contains(token)) throw new SuppressedTokenDataException("Unable to get the required token data for a given endec! [Token Name: " + token.name() + "]");

        var data = get(token);

        if(data == null) throw new MissingTokenDataException("Unable to get the required token data for a given endec! [Token Name: " + token.name() + "]");

        return data;
    }

    public <DATA_TYPE> boolean has(DataToken<DATA_TYPE> token) {
        return !suppressed().contains(token) && this.tokens().containsKey(token);
    }
}
