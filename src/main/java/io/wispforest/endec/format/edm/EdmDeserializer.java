package io.wispforest.endec.format.edm;

import io.wispforest.endec.*;
import io.wispforest.endec.data.DataTokens;
import io.wispforest.endec.data.ExtraDataContext;
import io.wispforest.endec.util.RecursiveDeserializer;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EdmDeserializer extends RecursiveDeserializer<EdmElement<?>> implements SelfDescribedDeserializer<EdmElement<?>> {

    protected EdmDeserializer(EdmElement<?> serialized) {
        super(serialized);
    }

    public static EdmDeserializer of(EdmElement<?> serialized) {
        return new EdmDeserializer(serialized);
    }

    // ---

    @Override
    public byte readByte(ExtraDataContext ctx) {
        return this.getValue().cast();
    }

    @Override
    public short readShort(ExtraDataContext ctx) {
        return this.getValue().cast();
    }

    @Override
    public int readInt(ExtraDataContext ctx) {
        return this.getValue().cast();
    }

    @Override
    public long readLong(ExtraDataContext ctx) {
        return this.getValue().cast();
    }

    // ---

    @Override
    public float readFloat(ExtraDataContext ctx) {
        return this.getValue().cast();
    }

    @Override
    public double readDouble(ExtraDataContext ctx) {
        return this.getValue().cast();
    }

    // ---

    @Override
    public int readVarInt(ExtraDataContext ctx) {
        return this.readInt(ctx);
    }

    @Override
    public long readVarLong(ExtraDataContext ctx) {
        return this.readLong(ctx);
    }

    // ---

    @Override
    public boolean readBoolean(ExtraDataContext ctx) {
        return this.getValue().cast();
    }

    @Override
    public String readString(ExtraDataContext ctx) {
        return this.getValue().cast();
    }

    @Override
    public byte[] readBytes(ExtraDataContext ctx) {
        return this.getValue().cast();
    }

    @Override
    public <V> Optional<V> readOptional(ExtraDataContext ctx, Endec<V> endec) {
        var optional = this.getValue().<Optional<EdmElement<?>>>cast();
        if (optional.isPresent()) {
            return this.frame(
                    optional::get,
                    () -> Optional.of(endec.decode(this, ctx)),
                    false
            );
        } else {
            return Optional.empty();
        }
    }

    // ---

    @Override
    public <E> Deserializer.Sequence<E> sequence(ExtraDataContext ctx, Endec<E> elementEndec) {
        return new Sequence<>(elementEndec, this.getValue().cast(), ctx);
    }

    @Override
    public <V> Deserializer.Map<V> map(ExtraDataContext ctx, Endec<V> valueEndec) {
        return new Map<>(valueEndec, this.getValue().cast(), ctx);
    }

    @Override
    public Deserializer.Struct struct() {
        return new Struct(this.getValue().cast());
    }

    // ---

    @Override
    public <S> void readAny(Serializer<S> visitor, ExtraDataContext ctx) {
        this.visit(visitor, ctx, this.getValue());
    }

    private <S> void visit(Serializer<S> visitor, ExtraDataContext ctx, EdmElement<?> value) {
        switch (value.type()) {
            case BYTE -> visitor.writeByte(ctx, value.cast());
            case SHORT -> visitor.writeShort(ctx, value.cast());
            case INT -> visitor.writeInt(ctx, value.cast());
            case LONG -> visitor.writeLong(ctx, value.cast());
            case FLOAT -> visitor.writeFloat(ctx, value.cast());
            case DOUBLE -> visitor.writeDouble(ctx, value.cast());
            case BOOLEAN -> visitor.writeBoolean(ctx, value.cast());
            case STRING -> visitor.writeString(ctx, value.cast());
            case BYTES -> visitor.writeBytes(ctx, value.cast());
            case OPTIONAL ->
                    visitor.writeOptional(ctx, Endec.<EdmElement<?>>of(this::visit, (deserializer, ctx1) -> null), value.cast());
            case SEQUENCE -> {
                try (var sequence = visitor.sequence(ctx, Endec.<EdmElement<?>>of(this::visit, (deserializer, ctx1) -> null), value.<List<EdmElement<?>>>cast().size())) {
                    value.<List<EdmElement<?>>>cast().forEach(sequence::element);
                }
            }
            case MAP -> {
                try (var map = visitor.map(ctx, Endec.<EdmElement<?>>of(this::visit, (deserializer, ctx1) -> null), value.<java.util.Map<String, EdmElement<?>>>cast().size())) {
                    value.<java.util.Map<String, EdmElement<?>>>cast().forEach(map::entry);
                }
            }
        }
    }

    // ---

    private class Sequence<V> implements Deserializer.Sequence<V> {

        private final Endec<V> valueEndec;
        private final ExtraDataContext ctx;

        private final Iterator<EdmElement<?>> elements;
        private final int size;

        private Sequence(Endec<V> valueEndec, List<EdmElement<?>> elements, ExtraDataContext ctx) {
            this.valueEndec = valueEndec;
            this.ctx = ctx;

            this.elements = elements.iterator();
            this.size = elements.size();
        }

        @Override
        public int estimatedSize() {
            return this.size;
        }

        @Override
        public boolean hasNext() {
            return this.elements.hasNext();
        }

        @Override
        public V next() {
            return EdmDeserializer.this.frame(
                    this.elements::next,
                    () -> this.valueEndec.decode(EdmDeserializer.this, ctx),
                    false
            );
        }
    }

    private class Map<V> implements Deserializer.Map<V> {

        private final Endec<V> valueEndec;
        private final ExtraDataContext ctx;

        private final Iterator<java.util.Map.Entry<String, EdmElement<?>>> entries;
        private final int size;

        private Map(Endec<V> valueEndec, java.util.Map<String, EdmElement<?>> entries, ExtraDataContext ctx) {
            this.valueEndec = valueEndec;
            this.ctx = ctx;

            this.entries = entries.entrySet().iterator();
            this.size = entries.size();
        }

        @Override
        public int estimatedSize() {
            return this.size;
        }

        @Override
        public boolean hasNext() {
            return this.entries.hasNext();
        }

        @Override
        public java.util.Map.Entry<String, V> next() {
            var entry = entries.next();
            return EdmDeserializer.this.frame(
                    entry::getValue,
                    () -> java.util.Map.entry(entry.getKey(), this.valueEndec.decode(EdmDeserializer.this, ctx)),
                    false
            );
        }
    }

    private class Struct implements Deserializer.Struct {

        private final java.util.Map<String, EdmElement<?>> map;

        private Struct(java.util.Map<String, EdmElement<?>> map) {
            this.map = map;
        }

        @Override
        public <F> @Nullable F field(ExtraDataContext ctx, String name, Endec<F> endec) {
            if (!this.map.containsKey(name)) {
                throw new IllegalStateException("Field '" + name + "' was missing from serialized data, but no default value was provided");
            }
            return EdmDeserializer.this.frame(
                    () -> this.map.get(name),
                    () -> endec.decode(EdmDeserializer.this, ctx),
                    true
            );
        }

        @Override
        public <F> @Nullable F field(ExtraDataContext ctx, String name, Endec<F> endec, @Nullable F defaultValue) {
            if (!this.map.containsKey(name)) return defaultValue;
            return EdmDeserializer.this.frame(
                    () -> this.map.get(name),
                    () -> endec.decode(EdmDeserializer.this, ctx),
                    true
            );
        }
    }
}
