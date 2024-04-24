package io.wispforest.endec.data;

import io.wispforest.endec.impl.MissingTokenDataException;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface ExtraDataContext {

    Map<DataToken<?>, Object> allTokens();

    default <DATA_TYPE> @Nullable DATA_TYPE get(DataToken<DATA_TYPE> token) {
        return (DATA_TYPE) this.allTokens().get(token);
    }

    default <DATA_TYPE> DATA_TYPE getOrThrow(DataToken<DATA_TYPE> token){
        var data = get(token);

        if(data == null) throw new MissingTokenDataException("Unable to get the required token data for a given endec! [Token Name: " + token.name() + "]");

        return data;
    }

    default <DATA_TYPE> boolean has(DataToken<DATA_TYPE> token) {
        return this.allTokens().containsKey(token);
    }
}
