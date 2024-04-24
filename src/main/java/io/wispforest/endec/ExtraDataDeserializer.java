package io.wispforest.endec;

import com.google.common.collect.ImmutableMap;
import io.wispforest.endec.data.DataToken;

public abstract class ExtraDataDeserializer<T> implements Deserializer<T> {

    protected final java.util.Map<DataToken<?>, Object> contextData;

    protected ExtraDataDeserializer(DataToken.Instance ...instances) {
        this.contextData = ImmutableMap.copyOf(DataToken.mappedData(instances));
    }

    protected ExtraDataDeserializer() {
        this.contextData = ImmutableMap.of();
    }

    @Override
    public java.util.Map<DataToken<?>, Object> allTokens() {
        return this.contextData;
    }
}
