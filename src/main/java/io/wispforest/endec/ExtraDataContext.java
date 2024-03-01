package io.wispforest.endec;

import org.jetbrains.annotations.Nullable;

public interface ExtraDataContext {

    <DATA_TYPE> @Nullable DATA_TYPE get(DataToken<DATA_TYPE> token);

    <DATA_TYPE> boolean has(DataToken<DATA_TYPE> token);

    <DATA_TYPE> void set(DataToken<DATA_TYPE> token, DATA_TYPE data);

    <DATA_TYPE> void remove(DataToken<DATA_TYPE> token);
}
