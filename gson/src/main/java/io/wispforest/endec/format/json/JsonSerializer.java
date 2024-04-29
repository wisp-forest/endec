package io.wispforest.endec.format.json;

import com.google.gson.*;
import io.wispforest.endec.SelfDescribedSerializer;
import io.wispforest.endec.data.DataTokens;
import io.wispforest.endec.Endec;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.data.SerializationContext;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.endec.util.RecursiveSerializer;

import java.util.Optional;

public class JsonSerializer extends RecursiveSerializer<JsonElement> implements SelfDescribedSerializer<JsonElement> {

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

    @Override
    public SerializationContext initalContext(SerializationContext ctx) {
        return ctx.with(DataTokens.HUMAN_READABLE);
    }


    // ---

    @Override
    public void writeByte(SerializationContext ctx, byte value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeShort(SerializationContext ctx, short value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeInt(SerializationContext ctx, int value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeLong(SerializationContext ctx, long value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeFloat(SerializationContext ctx, float value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeDouble(SerializationContext ctx, double value) {
        this.consume(new JsonPrimitive(value));
    }

    // ---

    @Override
    public void writeVarInt(SerializationContext ctx, int value) {
        this.writeInt(ctx, value);
    }

    @Override
    public void writeVarLong(SerializationContext ctx, long value) {
        this.writeLong(ctx, value);
    }

    // ---

    @Override
    public void writeBoolean(SerializationContext ctx, boolean value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeString(SerializationContext ctx, String value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeBytes(SerializationContext ctx, byte[] bytes) {
        var result = new JsonArray(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            result.add(bytes[i]);
        }

        this.consume(result);
    }

    @Override
    public <V> void writeOptional(SerializationContext ctx, Endec<V> endec, Optional<V> optional) {
        if (this.isWritingStructField()) {
            optional.ifPresent(value -> endec.encode(ctx, this, value));
        } else {
            optional.ifPresentOrElse(
                    value -> endec.encode(ctx, this, value),
                    () -> this.consume(JsonNull.INSTANCE)
            );
        }
    }

    // ---

    @Override
    public <E> Serializer.Sequence<E> sequence(SerializationContext ctx, Endec<E> elementEndec, int size) {
        return new Sequence<>(elementEndec, ctx, size);
    }

    @Override
    public <V> Serializer.Map<V> map(SerializationContext ctx, Endec<V> valueEndec, int size) {
        return new Map<>(valueEndec, ctx);
    }

    @Override
    public Struct struct() {
        return new Map<>(null, SerializationContext.of());
    }

    // ---

    private class Map<V> implements Serializer.Map<V>, Struct {

        private final Endec<V> valueEndec;
        private final SerializationContext ctx;

        private final JsonObject result;

        private Map(Endec<V> valueEndec, SerializationContext ctx) {
            this.valueEndec = valueEndec;
            this.ctx = ctx;

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
                this.valueEndec.encode(ctx, JsonSerializer.this, value);
                this.result.add(key, encoded.require("map value"));
            }, false);
        }

        @Override
        public <F> Struct field(SerializationContext ctx, String name, Endec<F> endec, F value) {
            JsonSerializer.this.frame(encoded -> {
                endec.encode(ctx, JsonSerializer.this, value);
                if (encoded.wasEncoded()) this.result.add(name, encoded.get());
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
        private final SerializationContext ctx;

        private final JsonArray result;

        private Sequence(Endec<V> valueEndec, SerializationContext ctx, int size) {
            this.valueEndec = valueEndec;
            this.ctx = ctx;

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
                this.valueEndec.encode(ctx, JsonSerializer.this, element);
                this.result.add(encoded.require("sequence element"));
            }, false);
        }

        @Override
        public void end() {
            JsonSerializer.this.consume(result);
        }
    }

    public static void main(String[] args) {
        var endec = StructEndecBuilder.of(
                Endec.STRING.optionalFieldOf("a_field", Bruh::aField, "a value"),
                Bruh::new
        );

        System.out.println(endec.encodeFully(JsonSerializer::of, new Bruh(null)).toString());
    }

    record Bruh(String aField) {}
}
