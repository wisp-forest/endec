package io.wispforest.endec.util;

import org.jetbrains.annotations.Nullable;

public class RangeNumberException extends RuntimeException {

    public final Number n;

    @Nullable
    public final Number lowerBound, upperBound;

    public RangeNumberException(Number n, @Nullable  Number lowerBound, @Nullable  Number upperBound) {
        super(createMsg(n, lowerBound, upperBound));

        this.n = n;

        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    private static String createMsg(Number n, @Nullable Number lowerBound, @Nullable Number upperBound) {
        String rangeMessage = "";

        if(lowerBound != null) rangeMessage += ", InclusiveMin: " + lowerBound;
        if(upperBound != null) rangeMessage += ", InclusiveMax: " + upperBound;

        return "Number value found to be outside allowed bound! [Value: " + n + (rangeMessage) + "]";
    }
}
