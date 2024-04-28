package io.wispforest.endec.format.json;

import blue.endless.jankson.*;
import io.wispforest.endec.SelfDescribedDeserializer;
import io.wispforest.endec.SelfDescribedSerializer;
import io.wispforest.endec.data.DataTokens;
import io.wispforest.endec.Endec;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.data.ExtraDataContext;
import io.wispforest.endec.format.edm.EdmElement;
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
    public ExtraDataContext initalContext(ExtraDataContext ctx) {
        if(!ctx.has(DataTokens.HUMAN_READABLE)) {
            return ctx.with(DataTokens.HUMAN_READABLE.with(true));
        }

        return ctx;
    }

    // ---

    @Override
    public void writeByte(ExtraDataContext ctx, byte value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeShort(ExtraDataContext ctx, short value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeInt(ExtraDataContext ctx, int value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeLong(ExtraDataContext ctx, long value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeFloat(ExtraDataContext ctx, float value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeDouble(ExtraDataContext ctx, double value) {
        this.consume(new JsonPrimitive(value));
    }

    // ---

    @Override
    public void writeVarInt(ExtraDataContext ctx, int value) {
        this.writeInt(ctx, value);
    }

    @Override
    public void writeVarLong(ExtraDataContext ctx, long value) {
        this.writeLong(ctx, value);
    }

    // ---

    @Override
    public void writeBoolean(ExtraDataContext ctx, boolean value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeString(ExtraDataContext ctx, String value) {
        this.consume(new JsonPrimitive(value));
    }

    @Override
    public void writeBytes(ExtraDataContext ctx, byte[] bytes) {
        var result = new JsonArray();
        for (int i = 0; i < bytes.length; i++) {
            result.add(new JsonPrimitive(bytes[i]));
        }

        this.consume(result);
    }

    @Override
    public <V> void writeOptional(ExtraDataContext ctx, Endec<V> endec, Optional<V> optional) {
        if (this.isWritingStructField()) {
            optional.ifPresent(value -> endec.encode(this, ctx, value));
        } else {
            optional.ifPresentOrElse(
                    value -> endec.encode(this, ctx, value),
                    () -> this.consume(JsonNull.INSTANCE)
            );
        }
    }

    // ---

    @Override
    public <E> Serializer.Sequence<E> sequence(ExtraDataContext ctx, Endec<E> elementEndec, int size) {
        return new Sequence<>(elementEndec, size, ctx);
    }

    @Override
    public <V> Serializer.Map<V> map(ExtraDataContext ctx, Endec<V> valueEndec, int size) {
        return new Map<>(valueEndec, ctx);
    }

    @Override
    public Struct struct() {
        return new Map<>(null, ExtraDataContext.of());
    }

    // ---

    private class Map<V> implements Serializer.Map<V>, Struct {

        private final Endec<V> valueEndec;
        private final JsonObject result;

        private final ExtraDataContext ctx;

        private Map(Endec<V> valueEndec, ExtraDataContext ctx) {
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
                this.valueEndec.encode(JsonSerializer.this, ctx, value);
                this.result.put(key, encoded.require("map value"));
            }, false);
        }

        @Override
        public <F> Struct field(ExtraDataContext ctx, String name, Endec<F> endec, F value) {
            JsonSerializer.this.frame(encoded -> {
                endec.encode(JsonSerializer.this, ctx, value);
                this.result.put(name, encoded.require("struct field"));
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

        private final ExtraDataContext ctx;

        private Sequence(Endec<V> valueEndec, int size, ExtraDataContext ctx) {
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
                this.result = new JsonArray();
            }
        }

        @Override
        public void element(V element) {
            JsonSerializer.this.frame(encoded -> {
                this.valueEndec.encode(JsonSerializer.this, ctx, element);
                this.result.add(encoded.require("sequence element"));
            }, false);
        }

        @Override
        public void end() {
            JsonSerializer.this.consume(result);
        }
    }
}
