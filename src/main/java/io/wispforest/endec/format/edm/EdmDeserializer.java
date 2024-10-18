package io.wispforest.endec.format.edm;

import io.wispforest.endec.*;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.util.RecursiveDeserializer;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class EdmDeserializer extends RecursiveDeserializer<EdmElement<?>> implements SelfDescribedDeserializer<EdmElement<?>> {

    protected EdmDeserializer(EdmElement<?> serialized) {
        super(serialized);
    }

    public static EdmDeserializer of(EdmElement<?> serialized) {
        return new EdmDeserializer(serialized);
    }

    // ---

    @Override
    public byte readByte(SerializationContext ctx) {
        return this.getValue().cast();
    }

    @Override
    public short readShort(SerializationContext ctx) {
        return this.getValue().cast();
    }

    @Override
    public int readInt(SerializationContext ctx) {
        return this.getValue().cast();
    }

    @Override
    public long readLong(SerializationContext ctx) {
        return this.getValue().cast();
    }

    // ---

    @Override
    public float readFloat(SerializationContext ctx) {
        return this.getValue().cast();
    }

    @Override
    public double readDouble(SerializationContext ctx) {
        return this.getValue().cast();
    }

    // ---

    @Override
    public int readVarInt(SerializationContext ctx) {
        return this.readInt(ctx);
    }

    @Override
    public long readVarLong(SerializationContext ctx) {
        return this.readLong(ctx);
    }

    // ---

    @Override
    public boolean readBoolean(SerializationContext ctx) {
        return this.getValue().cast();
    }

    @Override
    public String readString(SerializationContext ctx) {
        return this.getValue().cast();
    }

    @Override
    public byte[] readBytes(SerializationContext ctx) {
        return this.getValue().cast();
    }

    @Override
    public <V> Optional<V> readOptional(SerializationContext ctx, Endec<V> endec) {
        var optional = this.getValue().<Optional<EdmElement<?>>>cast();
        if (optional.isPresent()) {
            return this.frame(
                    optional::get,
                    () -> Optional.of(endec.decode(ctx, this))
            );
        } else {
            return Optional.empty();
        }
    }

    // ---

    @Override
    public <E> Deserializer.Sequence<E> sequence(SerializationContext ctx, Endec<E> elementEndec) {
        return new Sequence<>(ctx, elementEndec, this.getValue().cast());
    }

    @Override
    public <V> Deserializer.Map<V> map(SerializationContext ctx, Endec<V> valueEndec) {
        return new Map<>(ctx, valueEndec, this.getValue().cast());
    }

    @Override
    public Deserializer.Struct struct() {
        return new Struct(this.getValue().cast());
    }

    // ---

    @Override
    public <S> void readAny(SerializationContext ctx, Serializer<S> visitor) {
        this.visit(ctx, visitor, this.getValue());
    }

    private <S> void visit(SerializationContext ctx, Serializer<S> visitor, EdmElement<?> value) {
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
            case OPTIONAL -> visitor.writeOptional(ctx, Endec.<EdmElement<?>>of(this::visit, (ctx1, deserializer) -> null), value.cast());
            case SEQUENCE -> {
                try (var sequence = visitor.sequence(ctx, Endec.<EdmElement<?>>of(this::visit, (ctx1, deserializer) -> null), value.<List<EdmElement<?>>>cast().size())) {
                    value.<List<EdmElement<?>>>cast().forEach(sequence::element);
                }
            }
            case MAP -> {
                try (var map = visitor.map(ctx, Endec.<EdmElement<?>>of(this::visit, (ctx1, deserializer) -> null), value.<java.util.Map<String, EdmElement<?>>>cast().size())) {
                    value.<java.util.Map<String, EdmElement<?>>>cast().forEach(map::entry);
                }
            }
        }
    }

    // ---

    protected final class Sequence<V> implements Deserializer.Sequence<V> {

        private final SerializationContext ctx;
        private final Endec<V> valueEndec;
        private final Iterator<EdmElement<?>> elements;
        private final int size;

        Sequence(SerializationContext ctx, Endec<V> valueEndec, List<EdmElement<?>> elements) {
            this.ctx = ctx;
            this.valueEndec = valueEndec;

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
            var element = this.elements.next();
            return EdmDeserializer.this.frame(
                    () -> element,
                    () -> this.valueEndec.decode(this.ctx, EdmDeserializer.this)
            );
        }
    }

    private class Map<V> implements Deserializer.Map<V> {

        private final SerializationContext ctx;
        private final Endec<V> valueEndec;
        private final Iterator<java.util.Map.Entry<String, EdmElement<?>>> entries;
        private final int size;

        private Map(SerializationContext ctx, Endec<V> valueEndec, java.util.Map<String, EdmElement<?>> entries) {
            this.ctx = ctx;
            this.valueEndec = valueEndec;

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
            var entry = this.entries.next();
            return EdmDeserializer.this.frame(
                    entry::getValue,
                    () -> java.util.Map.entry(entry.getKey(), this.valueEndec.decode(this.ctx, EdmDeserializer.this))
            );
        }
    }

    private class Struct implements Deserializer.Struct {

        private final java.util.Map<String, EdmElement<?>> map;

        private Struct(java.util.Map<String, EdmElement<?>> map) {
            this.map = map;
        }

        @Override
        public <F> @Nullable F field(String name, SerializationContext ctx, Endec<F> endec, @Nullable Supplier<F> defaultValueFactory) {
            var element = this.map.get(name);
            if (element == null) {
                if(defaultValueFactory == null) {
                    throw new IllegalStateException("Field '" + name + "' was missing from serialized data, but no default value was provided");
                }

                return defaultValueFactory.get();
            }
            return EdmDeserializer.this.frame(
                    () -> element,
                    () -> endec.decode(ctx, EdmDeserializer.this)
            );
        }
    }
}
