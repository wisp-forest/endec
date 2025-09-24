package io.wispforest.endec.format.edm;

import io.wispforest.endec.*;
import io.wispforest.endec.util.RecursiveDeserializer;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

import static io.wispforest.endec.format.edm.EdmElement.Type.*;

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
        return this.getValueForType(ctx, I8);
    }

    @Override
    public short readShort(SerializationContext ctx) {
        return this.getValueForType(ctx, I16);
    }

    @Override
    public int readInt(SerializationContext ctx) {
        return this.getValueForType(ctx, I32);
    }

    @Override
    public long readLong(SerializationContext ctx) {
        return this.getValueForType(ctx, I64);
    }

    // ---

    @Override
    public float readFloat(SerializationContext ctx) {
        return this.getValueForType(ctx, F32);
    }

    @Override
    public double readDouble(SerializationContext ctx) {
        return this.getValueForType(ctx, F64);
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
        return this.getValueForType(ctx, BOOLEAN);
    }

    @Override
    public String readString(SerializationContext ctx) {
        return this.getValueForType(ctx, STRING);
    }

    @Override
    public byte[] readBytes(SerializationContext ctx) {
        return this.getValueForType(ctx, BYTES);
    }

    @Override
    public <V> Optional<V> readOptional(SerializationContext ctx, Endec<V> endec) {
        var optional = this.<Optional<EdmElement<?>>>getValueForType(ctx, OPTIONAL);
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
        return new Sequence<>(ctx, elementEndec, this.getValueForType(ctx, SEQUENCE));
    }

    @Override
    public <V> Deserializer.Map<V> map(SerializationContext ctx, Endec<V> valueEndec) {
        return new Map<>(ctx, valueEndec, this.getValueForType(ctx, MAP));
    }

    @Override
    public Deserializer.Struct struct(SerializationContext ctx) {
        return new Struct(this.getValueForType(ctx, MAP));
    }

    // ---

    protected <T> T getValueForType(SerializationContext ctx, EdmElement.Type type) {
        return getValueForType(ctx, List.of(type));
    }

    protected <T> T getValueForType(SerializationContext ctx, List<EdmElement.Type> types) {
        var value = getValue();

        if (!types.contains(value.type())) {
            var typeMessage = (types.size() == 1)
                ? "a " + types.get(0)
                : "any [" + String.join(",", types.stream().map(Object::toString).toList()) + "]";

            ctx.throwMalformedInput("Expected " + typeMessage + ", got a " + value.type().formatName());
        }

        return value.cast();
    }

    // ---

    @Override
    public <S> void readAny(SerializationContext ctx, Serializer<S> visitor) {
        this.visit(ctx, visitor, this.getValue());
    }

    private <S> void visit(SerializationContext ctx, Serializer<S> visitor, EdmElement<?> value) {
        switch (value.type()) {
            case I8 -> visitor.writeByte(ctx, value.cast());
            case I16 -> visitor.writeShort(ctx, value.cast());
            case I32 -> visitor.writeInt(ctx, value.cast());
            case I64 -> visitor.writeLong(ctx, value.cast());
            case F32 -> visitor.writeFloat(ctx, value.cast());
            case F64 -> visitor.writeDouble(ctx, value.cast());
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
        private final ListIterator<EdmElement<?>> elements;
        private final int size;

        Sequence(SerializationContext ctx, Endec<V> valueEndec, List<EdmElement<?>> elements) {
            this.ctx = ctx;
            this.valueEndec = valueEndec;

            this.elements = elements.listIterator();
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
            var index = this.elements.nextIndex();
            var element = this.elements.next();
            return EdmDeserializer.this.frame(
                    () -> element,
                    () -> this.valueEndec.decode(this.ctx.pushIndex(index), EdmDeserializer.this)
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
                    () -> java.util.Map.entry(entry.getKey(), this.valueEndec.decode(this.ctx.pushField(entry.getKey()), EdmDeserializer.this))
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
