package io.wispforest.endec.format.json;

import com.google.gson.*;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationAttribute;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.util.RecursiveSerializer;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public class JsonSerializer extends RecursiveSerializer<JsonElement> {

    private static final Set<SerializationAttribute> ATTRIBUTES = EnumSet.allOf(SerializationAttribute.class);
    private JsonElement prefix;

    protected JsonSerializer(JsonElement prefix) {
        super(null);
        this.prefix = prefix;
    }

    public static JsonSerializer of(JsonElement prefix) {
        return new JsonSerializer(prefix);
    }

    public static JsonSerializer of() {
        return of(null);
    }

    // ---

    @Override
    public Set<SerializationAttribute> attributes() {
        return ATTRIBUTES;
    }

    // ---

    @Override
    public void writeByte(byte value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeShort(short value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeInt(int value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeLong(long value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeFloat(float value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeDouble(double value) {
        this.consume(new JsonPrimitive(value));
    }

    // ---

    @Override
    public void writeVarInt(int value) {
        this.writeInt(value);
    }

    @Override
    public void writeVarLong(long value) {
        this.writeLong(value);
    }

    // ---

    @Override
    public void writeBoolean(boolean value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeString(String value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeBytes(byte[] bytes) {
        var result = new JsonArray(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            result.add(bytes[i]);
        }

        this.consume(result);
    }

    @Override
    public <V> void writeOptional(Endec<V> endec, Optional<V> optional) {
        if (this.isWritingStructField()) {
            optional.ifPresent(value -> endec.encode(this, value));
        } else {
            optional.ifPresentOrElse(
                    value -> endec.encode(this, value),
                    () -> this.consume(JsonNull.INSTANCE)
            );
        }
    }

    // ---

    @Override
    public <E> Serializer.Sequence<E> sequence(Endec<E> elementEndec, int size) {
        return new Sequence<>(elementEndec, size);
    }

    @Override
    public <V> Serializer.Map<V> map(Endec<V> valueEndec, int size) {
        return new Map<>(valueEndec);
    }

    @Override
    public Struct struct() {
        return new Map<>(null);
    }

    // ---

    private class Map<V> implements Serializer.Map<V>, Struct {

        private final Endec<V> valueEndec;
        private final JsonObject result;

        private Map(Endec<V> valueEndec) {
            this.valueEndec = valueEndec;

            if (JsonSerializer.this.prefix != null) {
                if (JsonSerializer.this.prefix instanceof JsonObject prefixObject) {
                    this.result = prefixObject;
                    JsonSerializer.this.prefix = null;
                } else {
                    throw new IllegalStateException("Incompatible prefix of type " + JsonSerializer.this.prefix.getClass().getSimpleName() + " used for JSON map/struct");
                }
            } else {
                this.result = new JsonObject();
            }
        }

        @Override
        public void entry(String key, V value) {
            JsonSerializer.this.frame(encoded -> {
                this.valueEndec.encode(JsonSerializer.this, value);
                this.result.add(key, encoded.require("map value"));
            }, false);
        }

        @Override
        public <F> Struct field(String name, Endec<F> endec, F value) {
            JsonSerializer.this.frame(encoded -> {
                endec.encode(JsonSerializer.this, value);
                this.result.add(name, encoded.require("struct field"));
            }, true);

            return this;
        }

        @Override
        public void end() {
            JsonSerializer.this.consume(result);
        }
    }

    private class Sequence<V> implements Serializer.Sequence<V> {

        private final Endec<V> valueEndec;
        private final JsonArray result;

        private Sequence(Endec<V> valueEndec, int size) {
            this.valueEndec = valueEndec;

            if (JsonSerializer.this.prefix != null) {
                if (JsonSerializer.this.prefix instanceof JsonArray prefixArray) {
                    this.result = prefixArray;
                    JsonSerializer.this.prefix = null;
                } else {
                    throw new IllegalStateException("Incompatible prefix of type " + JsonSerializer.this.prefix.getClass().getSimpleName() + " used for JSON sequence");
                }
            } else {
                this.result = new JsonArray(size);
            }
        }

        @Override
        public void element(V element) {
            JsonSerializer.this.frame(encoded -> {
                this.valueEndec.encode(JsonSerializer.this, element);
                this.result.add(encoded.require("sequence element"));
            }, false);
        }

        @Override
        public void end() {
            JsonSerializer.this.consume(result);
        }
    }
}
