package io.wispforest.endec;

import com.google.common.collect.ImmutableMap;
import io.wispforest.endec.data.DataToken;

public abstract class ExtraDataSerializer<T> implements Serializer<T> {

    private final java.util.Map<DataToken<?>, Object> contextData;

    protected ExtraDataSerializer(DataToken.Instance ...instances){
        this.contextData = ImmutableMap.copyOf(DataToken.mappedData(instances));
    }

    protected ExtraDataSerializer(){
        this.contextData = ImmutableMap.of();
    }

    @Override
    public java.util.Map<DataToken<?>, Object> allTokens() {
        return this.contextData;
    }
}
