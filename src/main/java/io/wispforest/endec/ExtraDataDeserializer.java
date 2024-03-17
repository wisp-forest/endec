package io.wispforest.endec;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class ExtraDataDeserializer<T> implements Deserializer<T> {

    private final java.util.Map<DataToken<?>, Object> contextData = new HashMap<>();

    @Override
    public Set<DataTokenHolder<?>> allTokens() {
        return this.contextData.entrySet().stream()
                .map(entry -> entry.getKey().holderFromUnsafe(entry.getValue()))
                .collect(Collectors.toSet());
    }

    @Override
    @Nullable
    public <DATA_TYPE> DATA_TYPE get(DataToken<DATA_TYPE> token) {
        return (DATA_TYPE) this.contextData.get(token);
    }

    @Override
    public <DATA_TYPE> void set(DataToken<DATA_TYPE> token, DATA_TYPE data) {
        this.contextData.put(token, data);
    }

    @Override
    public <DATA_TYPE> boolean has(DataToken<DATA_TYPE> token) {
        return this.contextData.containsKey(token);
    }

    @Override
    public <DATA_TYPE> void remove(DataToken<DATA_TYPE> token) {
        this.contextData.remove(token);
    }
}
