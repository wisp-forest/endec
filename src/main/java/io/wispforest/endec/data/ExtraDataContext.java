package io.wispforest.endec.data;

import com.google.common.collect.ImmutableMap;
import io.wispforest.endec.impl.MissingTokenDataException;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public interface ExtraDataContext {

    static ExtraDataContext of(DataToken.Instance ...instances){
        return new ExtraDataContextImpl(Map.of()).with(instances);
    }

    Map<DataToken<?>, Object> tokens();

    default DataToken.Instance[] instances(){
        return DataToken.streamedData(tokens()).toArray(DataToken.Instance[]::new);
    }

    default ExtraDataContext with(DataToken.Instance ...instances) {
        var map = new HashMap<>(tokens());

        for (var instance : instances) {
            map.put(instance.getToken(), instance.getValue());
        }

        return new ExtraDataContextImpl(ImmutableMap.copyOf(map));
    }

    default <DATA_TYPE> @Nullable DATA_TYPE get(DataToken<DATA_TYPE> token) {
        return (DATA_TYPE) this.tokens().get(token);
    }

    default <DATA_TYPE> DATA_TYPE getOrThrow(DataToken<DATA_TYPE> token){
        var data = get(token);

        if(data == null) throw new MissingTokenDataException("Unable to get the required token data for a given endec! [Token Name: " + token.name() + "]");

        return data;
    }

    default <DATA_TYPE> boolean has(DataToken<DATA_TYPE> token) {
        return this.tokens().containsKey(token);
    }
}
