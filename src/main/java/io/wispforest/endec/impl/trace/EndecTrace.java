package io.wispforest.endec.impl.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EndecTrace {
    private final List<EndecTraceElement> elements;

    public EndecTrace(List<EndecTraceElement> elements) {
        this.elements = elements;
    }

    public EndecTrace() {
        this(List.of());
    }

    public EndecTrace push(EndecTraceElement element) {
        var newElements = new ArrayList<>(elements);

        newElements.add(element);

        return new EndecTrace(newElements);
    }

    @Override
    public String toString() {
        return "$" + elements.stream()
            .map(EndecTraceElement::toFormatedString)
            .collect(Collectors.joining());
    }
}

