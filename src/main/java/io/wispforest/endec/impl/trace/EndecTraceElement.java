package io.wispforest.endec.impl.trace;

public sealed interface EndecTraceElement {
    String toFormatedString();

    record FieldTraceElement(String name) implements EndecTraceElement {
        @Override
        public String toFormatedString() { return "." + name; }
    }

    record IndexTraceElement(int index) implements EndecTraceElement {
        @Override
        public String toFormatedString() { return "[" + index + "]"; }
    }
}
