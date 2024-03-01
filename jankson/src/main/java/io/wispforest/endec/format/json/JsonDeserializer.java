package io.wispforest.endec.format.json;


import blue.endless.jankson.*;
import io.wispforest.endec.*;
import io.wispforest.endec.util.RecursiveDeserializer;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

public class JsonDeserializer extends RecursiveDeserializer<JsonElement> implements SelfDescribedDeserializer<JsonElement> {

    private static final Set<SerializationAttribute> ATTRIBUTES = EnumSet.of(
            SerializationAttribute.SELF_DESCRIBING,
            SerializationAttribute.HUMAN_READABLE
    );

    protected JsonDeserializer(JsonElement serialized) {
        super(serialized);
    }

    public static JsonDeserializer of(JsonElement serialized) {
        return new JsonDeserializer(serialized);
    }

    // ---

    @Override
    public Set<SerializationAttribute> attributes() {
        return ATTRIBUTES;
    }

    // ---

    @Override
    public byte readByte() {
        return readPrimitive(Byte.class);
    }

    @Override
    public short readShort() {
        return readPrimitive(Short.class);
    }

    @Override
    public int readInt() {
        return readPrimitive(Integer.class);
    }

    @Override
    public long readLong() {
        return readPrimitive(Long.class);
    }

    @Override
    public float readFloat() {
        return readPrimitive(Float.class);
    }

    @Override
    public double readDouble() {
        return readPrimitive(Double.class);
    }

    private <T> T readPrimitive(Class<T> clazz){
        return clazz.cast(((JsonPrimitive) this.getValue()).getValue());
    }

    // ---

    @Override
    public int readVarInt() {
        return this.readInt();
    }

    @Override
    public long readVarLong() {
        return this.readLong();
    }

    // ---

    @Override
    public boolean readBoolean() {
        return readPrimitive(Boolean.class);
    }

    @Override
    public String readString() {
        return readPrimitive(String.class);
    }

    @Override
    public byte[] readBytes() {
        var array = ((JsonArray) this.getValue()).toArray();

        var result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = ((Number) ((JsonPrimitive) array[i]).getValue()).byteValue();
        }

        return result;
    }

    @Override
    public <V> Optional<V> readOptional(Endec<V> endec) {
        var value = this.getValue();
        return !(value instanceof JsonNull)
                ? Optional.of(endec.decode(this))
                : Optional.empty();
    }

    // ---

    @Override
    public <E> Deserializer.Sequence<E> sequence(Endec<E> elementEndec) {
        return new Sequence<>(elementEndec, (JsonArray) this.getValue());
    }

    @Override
    public <V> Deserializer.Map<V> map(Endec<V> valueEndec) {
        return new Map<>(valueEndec, ((JsonObject) this.getValue()));
    }

    @Override
    public Deserializer.Struct struct() {
        return new Struct((JsonObject) this.getValue());
    }

    // ---

    @Override
    public <S> void readAny(Serializer<S> visitor) {
        this.decodeValue(visitor, this.getValue());
    }

    private <S> void decodeValue(Serializer<S> visitor, JsonElement element) {
        if (element instanceof JsonNull) {
            visitor.writeOptional(JsonEndec.INSTANCE, Optional.empty());
        } else if (element instanceof JsonPrimitive primitive) {
            if (primitive.getValue() instanceof String s) {
                visitor.writeString(s);
            } else if (primitive.getValue() instanceof Boolean b) {
                visitor.writeBoolean(b);
            } else {
                var value = primitive.asBigDecimal(BigDecimal.ZERO);

                try {
                    var asLong = value.longValueExact();

                    if ((byte) asLong == asLong) {
                        visitor.writeByte((byte) primitive.getValue());
                    } else if ((short) asLong == asLong) {
                        visitor.writeShort((short) primitive.getValue());
                    } else if ((int) asLong == asLong) {
                        visitor.writeInt((int) primitive.getValue());
                    } else {
                        visitor.writeLong(asLong);
                    }
                } catch (ArithmeticException bruh /* quite cringe java moment, why use an exception for this */) {
                    var asDouble = value.doubleValue();

                    if ((float) asDouble == asDouble) {
                        visitor.writeFloat((float) primitive.getValue());
                    } else {
                        visitor.writeDouble(asDouble);
                    }
                }
            }
        } else if (element instanceof JsonArray array) {
            try (var sequence = visitor.sequence(Endec.<JsonElement>of(this::decodeValue, deserializer -> null), array.size())) {
                array.forEach(sequence::element);
            }
        } else if (element instanceof JsonObject object) {
            try (var map = visitor.map(Endec.<JsonElement>of(this::decodeValue, deserializer -> null), object.size())) {
                object.forEach(map::entry);
            }
        } else {
            throw new IllegalArgumentException("Non-standard, unrecognized JsonElement implementation cannot be decoded");
        }
    }

    // ---

    private class Sequence<V> implements Deserializer.Sequence<V> {

        private final Endec<V> valueEndec;
        private final Iterator<JsonElement> elements;
        private final int size;

        private Sequence(Endec<V> valueEndec, JsonArray elements) {
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
            return JsonDeserializer.this.frame(
                    this.elements::next,
                    () -> this.valueEndec.decode(JsonDeserializer.this),
                    false
            );
        }
    }

    private class Map<V> implements Deserializer.Map<V> {

        private final Endec<V> valueEndec;
        private final Iterator<java.util.Map.Entry<String, JsonElement>> entries;
        private final int size;

        private Map(Endec<V> valueEndec, JsonObject entries) {
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
            var entry = entries.next();
            return JsonDeserializer.this.frame(
                    entry::getValue,
                    () -> java.util.Map.entry(entry.getKey(), this.valueEndec.decode(JsonDeserializer.this)),
                    false
            );
        }
    }

    private class Struct implements Deserializer.Struct {

        private final JsonObject object;

        private Struct(JsonObject object) {
            this.object = object;
        }

        @Override
        public <F> @Nullable F field(String name, Endec<F> endec) {
            if (!this.object.containsKey(name)) {
                throw new IllegalStateException("Field '" + name + "' was missing from serialized data, but no default value was provided");
            }
            return JsonDeserializer.this.frame(
                    () -> this.object.get(name),
                    () -> endec.decode(JsonDeserializer.this),
                    true
            );
        }

        @Override
        public <F> @Nullable F field(String name, Endec<F> endec, @Nullable F defaultValue) {
            if (!this.object.containsKey(name)) return defaultValue;
            return JsonDeserializer.this.frame(
                    () -> this.object.get(name),
                    () -> endec.decode(JsonDeserializer.this),
                    true
            );
        }
    }
}
