package io.wispforest.endec;

import io.wispforest.endec.impl.MissingTokenDataException;
import org.jetbrains.annotations.Nullable;

public interface ExtraDataContext {

    <DATA_TYPE> @Nullable DATA_TYPE get(DataToken<DATA_TYPE> token);

    default <DATA_TYPE> DATA_TYPE getOrThrow(DataToken<DATA_TYPE> token){
        var data = get(token);

        if(data == null) throw new MissingTokenDataException("Unable to get the required token data for a given endec! [Token Name: " + token.name() + "]");

        return data;
    }

    <DATA_TYPE> boolean has(DataToken<DATA_TYPE> token);

    <DATA_TYPE> void set(DataToken<DATA_TYPE> token, DATA_TYPE data);

    <DATA_TYPE> void remove(DataToken<DATA_TYPE> token);
}
