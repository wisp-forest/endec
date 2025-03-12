package io.wispforest.endec.util.reflection;

import io.wispforest.endec.Endec;

public record AdjustmentResult<T>(Endec<T> endec, boolean allowFutherAdjustments) {
    public static AdjustmentResult<Void> NO_RESULT = new AdjustmentResult<>(null, true);

    public static <T> AdjustmentResult<T> empty() {
        return (AdjustmentResult<T>) AdjustmentResult.NO_RESULT;
    }

    public static <T> AdjustmentResult<T> of(Endec<T> endec) {
        return new AdjustmentResult<>(endec, true);
    }

    public static <T> AdjustmentResult<T> of(Endec<T> endec, boolean allowFutherAdjustments){
        return new AdjustmentResult<>(endec, allowFutherAdjustments);
    }
}
