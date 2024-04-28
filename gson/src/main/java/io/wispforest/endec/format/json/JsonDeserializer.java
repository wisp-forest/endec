package io.wispforest.endec.format.json;


import com.google.gson.*;
import io.wispforest.endec.*;
import io.wispforest.endec.data.DataToken;
import io.wispforest.endec.data.DataTokens;
import io.wispforest.endec.data.ExtraDataContext;
import io.wispforest.endec.util.RecursiveDeserializer;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class JsonDeserializer extends RecursiveDeserializer<JsonElement> implements SelfDescribedDeserializer<JsonElement> {

    protected JsonDeserializer(JsonElement serialized) {
        super(serialized);
    }

    public static JsonDeserializer of(JsonElement serialized) {
        return new JsonDeserializer(serialized);
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
    public byte readByte(ExtraDataContext ctx) {
        return this.getValue().getAsByte();
    }

    @Override
    public short readShort(ExtraDataContext ctx) {
        return this.getValue().getAsShort();
    }

    @Override
    public int readInt(ExtraDataContext ctx) {
        return this.getValue().getAsInt();
    }

    @Override
    public long readLong(ExtraDataContext ctx) {
        return this.getValue().getAsLong();
    }

    @Override
    public float readFloat(ExtraDataContext ctx) {
        return this.getValue().getAsFloat();
    }

    @Override
    public double readDouble(ExtraDataContext ctx) {
        return this.getValue().getAsDouble();
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
        return this.getValue().getAsBoolean();
    }

    @Override
    public String readString(ExtraDataContext ctx) {
        return this.getValue().getAsString();
    }

    @Override
    public byte[] readBytes(ExtraDataContext ctx) {
        var array = this.getValue().getAsJsonArray().asList();

        var result = new byte[array.size()];
        for (int i = 0; i < array.size(); i++) {
            result[i] = array.get(i).getAsByte();
        }

        return result;
    }

    @Override
    public <V> Optional<V> readOptional(ExtraDataContext ctx, Endec<V> endec) {
        var value = this.getValue();
        return !value.isJsonNull()
                ? Optional.of(endec.decode(this, ctx))
                : Optional.empty();
    }

    // ---

    @Override
    public <E> Deserializer.Sequence<E> sequence(ExtraDataContext ctx, Endec<E> elementEndec) {
        return new Sequence<>(elementEndec, (JsonArray) this.getValue(), ctx);
    }

    @Override
    public <V> Deserializer.Map<V> map(ExtraDataContext ctx, Endec<V> valueEndec) {
        return new Map<>(valueEndec, ((JsonObject) this.getValue()), ctx);
    }

    @Override
    public Deserializer.Struct struct() {
        return new Struct((JsonObject) this.getValue());
    }

    // ---

    @Override
    public <S> void readAny(Serializer<S> visitor, ExtraDataContext ctx) {
        this.decodeValue(visitor, ctx, this.getValue());
    }

    private <S> void decodeValue(Serializer<S> visitor, ExtraDataContext ctx, JsonElement element) {
        if (element.isJsonNull()) {
            visitor.writeOptional(ctx, JsonEndec.INSTANCE, Optional.empty());
        } else if (element instanceof JsonPrimitive primitive) {
            if (primitive.isString()) {
                visitor.writeString(ctx, primitive.getAsString());
            } else if (primitive.isBoolean()) {
                visitor.writeBoolean(ctx, primitive.getAsBoolean());
            } else {
                var value = primitive.getAsBigDecimal();

                try {
                    var asLong = value.longValueExact();

                    if ((byte) asLong == asLong) {
                        visitor.writeByte(ctx, element.getAsByte());
                    } else if ((short) asLong == asLong) {
                        visitor.writeShort(ctx, element.getAsShort());
                    } else if ((int) asLong == asLong) {
                        visitor.writeInt(ctx, element.getAsInt());
                    } else {
                        visitor.writeLong(ctx, asLong);
                    }
                } catch (ArithmeticException bruh /* quite cringe java moment, why use an exception for this */) {
                    var asDouble = value.doubleValue();

                    if ((float) asDouble == asDouble) {
                        visitor.writeFloat(ctx, element.getAsFloat());
                    } else {
                        visitor.writeDouble(ctx, asDouble);
                    }
                }
            }
        } else if (element instanceof JsonArray array) {
            try (var sequence = visitor.sequence(ctx, Endec.<JsonElement>of(this::decodeValue, (deserializer, ctx1) -> null), array.size())) {
                array.forEach(sequence::element);
            }
        } else if (element instanceof JsonObject object) {
            try (var map = visitor.map(ctx, Endec.<JsonElement>of(this::decodeValue, (deserializer, ctx1) -> null), object.size())) {
                object.asMap().forEach(map::entry);
            }
        } else {
            throw new IllegalArgumentException("Non-standard, unrecognized JsonElement implementation cannot be decoded");
        }
    }

    // ---

    private class Sequence<V> implements Deserializer.Sequence<V> {

        private final Endec<V> valueEndec;
        private final ExtraDataContext ctx;

        private final Iterator<JsonElement> elements;
        private final int size;

        private Sequence(Endec<V> valueEndec, JsonArray elements, ExtraDataContext ctx) {
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
            return JsonDeserializer.this.frame(
                    this.elements::next,
                    () -> this.valueEndec.decode(JsonDeserializer.this, ctx),
                    false
            );
        }
    }

    private class Map<V> implements Deserializer.Map<V> {

        private final Endec<V> valueEndec;
        private final ExtraDataContext ctx;

        private final Iterator<java.util.Map.Entry<String, JsonElement>> entries;

        private final int size;

        private Map(Endec<V> valueEndec, JsonObject entries, ExtraDataContext ctx) {
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
            return JsonDeserializer.this.frame(
                    entry::getValue,
                    () -> java.util.Map.entry(entry.getKey(), this.valueEndec.decode(JsonDeserializer.this, ctx)),
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
        public <F> @Nullable F field(ExtraDataContext ctx, String name, Endec<F> endec) {
            if (!this.object.has(name)) {
                throw new IllegalStateException("Field '" + name + "' was missing from serialized data, but no default value was provided");
            }
            return JsonDeserializer.this.frame(
                    () -> this.object.get(name),
                    () -> endec.decode(JsonDeserializer.this, ctx),
                    true
            );
        }

        @Override
        public <F> @Nullable F field(ExtraDataContext ctx, String name, Endec<F> endec, @Nullable F defaultValue) {
            if (!this.object.has(name)) return defaultValue;
            return JsonDeserializer.this.frame(
                    () -> this.object.get(name),
                    () -> endec.decode(JsonDeserializer.this, ctx),
                    true
            );
        }
    }
}
