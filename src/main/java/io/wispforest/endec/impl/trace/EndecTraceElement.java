package io.wispforest.endec.impl.trace;

public sealed interface EndecTraceElement {
    String valueAsString();

    String toFormatedString();

    record FieldTraceElement(String name) implements EndecTraceElement {
        @Override
        public String valueAsString() { return name; }

        @Override
        public String toFormatedString() { return "." + name; }
    }

    record IndexTraceElement(int index) implements EndecTraceElement {
        @Override
        public String valueAsString() { return String.valueOf(index); }

        @Override
        public String toFormatedString() { return "[" + index + "]"; }
    }
}
