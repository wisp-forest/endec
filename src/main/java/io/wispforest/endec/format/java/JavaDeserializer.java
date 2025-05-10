package io.wispforest.endec.format.java;

import io.wispforest.endec.*;
import io.wispforest.endec.util.RecursiveDeserializer;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class JavaDeserializer extends RecursiveDeserializer<Object> implements SelfDescribedDeserializer<Object> {

    protected JavaDeserializer(Object serialized) {
        super(serialized);
    }

    public static JavaDeserializer of(Object serialized) {
        return new JavaDeserializer(serialized);
    }

    @Override
    public byte readByte(SerializationContext ctx) {
        return getAndCast(byte.class);
    }

    @Override
    public short readShort(SerializationContext ctx) {
        return getAndCast(short.class);
    }

    @Override
    public int readInt(SerializationContext ctx) {
        return getAndCast(int.class);
    }

    @Override
    public long readLong(SerializationContext ctx) {
        return getAndCast(long.class);
    }

    @Override
    public float readFloat(SerializationContext ctx) {
        return getAndCast(float.class);
    }

    @Override
    public double readDouble(SerializationContext ctx) {
        return getAndCast(double.class);
    }

    @Override
    public int readVarInt(SerializationContext ctx) {
        return readInt(ctx);
    }

    @Override
    public long readVarLong(SerializationContext ctx) {
        return readLong(ctx);
    }

    @Override
    public boolean readBoolean(SerializationContext ctx) {
        return getAndCast(boolean.class);
    }

    @Override
    public String readString(SerializationContext ctx) {
        return getAndCast(String.class);
    }

    @Override
    public byte[] readBytes(SerializationContext ctx) {
        return getAndCast("byte[]", object -> object instanceof byte[], object -> (byte[]) object);
    }

    @Override
    public <V> Optional<V> readOptional(SerializationContext ctx, Endec<V> endec) {
        return getAndCast("Optional<?>", object -> object instanceof Optional<?>, object -> (Optional<V>) object);
    }

    @Override
    public <E> Deserializer.Sequence<E> sequence(SerializationContext ctx, Endec<E> elementEndec) {
        return new Sequence<>(ctx, elementEndec, this.getAndCast("List<?>", object -> object instanceof List<?>, object -> (List<Object>) object));
    }

    @Override
    public <V> Deserializer.Map<V> map(SerializationContext ctx, Endec<V> valueEndec) {
        return new Map<>(ctx, valueEndec, this.getAndCast("Map<String, ?>", object -> object instanceof java.util.Map<?,?>, object -> (java.util.Map<String, Object>) object));
    }

    @Override
    public Deserializer.Struct struct() {
        return new Struct(this.getAndCast("Map<String, ?>", object -> object instanceof java.util.Map<?,?>, object -> (java.util.Map<String, Object>) object));
    }

    // ---

    @Override
    public <S> void readAny(SerializationContext ctx, Serializer<S> visitor) {
        this.visit(ctx, visitor, this.getValue());
    }

    private <S> void visit(SerializationContext ctx, Serializer<S> visitor, Object value) {
        if (value instanceof Byte castedValue) {
            visitor.writeByte(ctx, castedValue);
        } else if(value instanceof Short castedValue) {
            visitor.writeShort(ctx, castedValue);
        } else if(value instanceof Integer castedValue) {
            visitor.writeInt(ctx, castedValue);
        } else if(value instanceof Long castedValue) {
            visitor.writeLong(ctx, castedValue);
        } else if(value instanceof Float castedValue) {
            visitor.writeFloat(ctx, castedValue);
        } else if(value instanceof Double castedValue) {
            visitor.writeDouble(ctx, castedValue);
        } else if(value instanceof Boolean castedValue) {
            visitor.writeBoolean(ctx, castedValue);
        } else if(value instanceof String castedValue) {
            visitor.writeString(ctx, castedValue);
        } else if(value instanceof byte[] castedValue) {
            visitor.writeBytes(ctx, castedValue);
        } else if(value instanceof Optional<?> castedValue) {
            visitor.writeOptional(ctx, Endec.of(this::visit, (ctx1, deserializer) -> null), (Optional<Object>) castedValue);
        } else if(value instanceof List<?> list) {
            var typedList = ((List<Object>) list);

            try (var sequence = visitor.sequence(ctx, Endec.of(this::visit, (ctx1, deserializer) -> null), typedList.size())) {
                typedList.forEach(sequence::element);
            }
        } else if(value instanceof java.util.Map<?,?> map) {
            var typedMap = ((java.util.Map<String, Object>) map);
            try (var mapDeserializer = visitor.map(ctx, Endec.of(this::visit, (ctx1, deserializer) -> null), typedMap.size())) {
                typedMap.forEach(mapDeserializer::entry);
            }
        } else {
            throw new IllegalStateException("Unable to handle the following Java type: " + value);
        }
    }

    private <T> T getAndCast(Class<T> clazz) {
        return getAndCast(clazz.getSimpleName(), object -> clazz.isAssignableFrom(object.getClass()), clazz::cast);
    }

    private <T> T getAndCast(String clazzName, Predicate<Object> isCompatible, Function<Object, T> cast) {
        var value = getValue();

        if (value == null) {
            throw new NullPointerException("Unable to get the value as [" + clazzName + "] since the value is currently null!");
        }

        if (!isCompatible.test(value)) {
            throw new IllegalStateException("Unable to cast the given value [" + value + "] to type of [" + clazzName + "] as such is not the compatible type!");
        }

        try {
            return cast.apply(value);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to cast the given java value [" + value + "] to the desired type [" + clazzName + "] as an exception has occurred: ", e);
        }
    }

    protected final class Sequence<V> implements Deserializer.Sequence<V> {

        private final SerializationContext ctx;
        private final Endec<V> valueEndec;
        private final Iterator<Object> elements;
        private final int size;

        Sequence(SerializationContext ctx, Endec<V> valueEndec, List<Object> elements) {
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
            return JavaDeserializer.this.frame(
                    () -> element,
                    () -> this.valueEndec.decode(this.ctx, JavaDeserializer.this)
            );
        }
    }

    private class Map<V> implements Deserializer.Map<V> {

        private final SerializationContext ctx;
        private final Endec<V> valueEndec;
        private final Iterator<java.util.Map.Entry<String, Object>> entries;
        private final int size;

        private Map(SerializationContext ctx, Endec<V> valueEndec, java.util.Map<String, Object> entries) {
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
            return JavaDeserializer.this.frame(
                    entry::getValue,
                    () -> java.util.Map.entry(entry.getKey(), this.valueEndec.decode(this.ctx, JavaDeserializer.this))
            );
        }
    }

    private class Struct implements Deserializer.Struct {

        private final java.util.Map<String, Object> map;

        private Struct(java.util.Map<String, Object> map) {
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
            return JavaDeserializer.this.frame(
                    () -> element,
                    () -> endec.decode(ctx, JavaDeserializer.this)
            );
        }
    }
}
