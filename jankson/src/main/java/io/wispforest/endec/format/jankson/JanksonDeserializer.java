package io.wispforest.endec.format.jankson;

import blue.endless.jankson.*;
import io.wispforest.endec.*;
import io.wispforest.endec.util.RecursiveDeserializer;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Supplier;

public class JanksonDeserializer extends RecursiveDeserializer<JsonElement> implements SelfDescribedDeserializer<JsonElement> {

    protected JanksonDeserializer(JsonElement serialized) {
        super(serialized);
    }

    public static JanksonDeserializer of(JsonElement serialized) {
        return new JanksonDeserializer(serialized);
    }

    @Override
    public SerializationContext setupContext(SerializationContext ctx) {
        return super.setupContext(ctx).withAttributes(SerializationAttributes.HUMAN_READABLE);
    }

    // ---

    @Override
    public byte readByte(SerializationContext ctx) {
        return readPrimitive(ctx, Number.class).byteValue();
    }

    @Override
    public short readShort(SerializationContext ctx) {
        return readPrimitive(ctx, Number.class).shortValue();
    }

    @Override
    public int readInt(SerializationContext ctx) {
        return readPrimitive(ctx, Number.class).intValue();
    }

    @Override
    public long readLong(SerializationContext ctx) {
        return readPrimitive(ctx, Number.class).longValue();
    }

    @Override
    public float readFloat(SerializationContext ctx) {
        return readPrimitive(ctx, Number.class).floatValue();
    }

    @Override
    public double readDouble(SerializationContext ctx) {
        return readPrimitive(ctx, Number.class).doubleValue();
    }

    private <T> T readPrimitive(SerializationContext ctx, Class<T> clazz){
        return clazz.cast(this.verifyAndGetValue(ctx, JsonPrimitive.class).getValue());
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
        return readPrimitive(ctx, Boolean.class);
    }

    @Override
    public String readString(SerializationContext ctx) {
        return readPrimitive(ctx, String.class);
    }

    @Override
    public byte[] readBytes(SerializationContext ctx) {
        var array = this.verifyAndGetValue(ctx, JsonArray.class).toArray();

        var result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = ((Number) ((JsonPrimitive) array[i]).getValue()).byteValue();
        }

        return result;
    }

    @Override
    public <V> Optional<V> readOptional(SerializationContext ctx, Endec<V> endec) {
        var value = this.getValue();
        return !(value instanceof JsonNull)
                ? Optional.of(endec.decode(ctx, this))
                : Optional.empty();
    }

    // ---

    @Override
    public <E> Deserializer.Sequence<E> sequence(SerializationContext ctx, Endec<E> elementEndec) {
        return new Sequence<>(ctx, elementEndec, this.verifyAndGetValue(ctx, JsonArray.class));
    }

    @Override
    public <V> Deserializer.Map<V> map(SerializationContext ctx, Endec<V> valueEndec) {
        return new Map<>(ctx, valueEndec, this.verifyAndGetValue(ctx, JsonObject.class));
    }

    @Override
    public Deserializer.Struct struct(SerializationContext ctx) {
        return new Struct(this.verifyAndGetValue(ctx, JsonObject.class));
    }

    // ---

    private <T extends JsonElement> T verifyAndGetValue(SerializationContext ctx, Class<T> clazz) {
        var value = getValue();

        if (!clazz.isInstance(value)) {
            ctx.throwMalformedInput("Expected " + clazz.getSimpleName() + ", got a " + value.getClass().getSimpleName());
        }

        return (T) value;
    }

    // ---

    @Override
    public <S> void readAny(SerializationContext ctx, Serializer<S> visitor) {
        this.decodeValue(ctx, visitor, this.getValue());
    }

    private <S> void decodeValue(SerializationContext ctx, Serializer<S> visitor, JsonElement element) {
        if (element instanceof JsonNull) {
            visitor.writeOptional(ctx, JanksonEndec.INSTANCE, Optional.empty());
        } else if (element instanceof JsonPrimitive primitive) {
            if (primitive.getValue() instanceof String s) {
                visitor.writeString(ctx, s);
            } else if (primitive.getValue() instanceof Boolean b) {
                visitor.writeBoolean(ctx, b);
            } else {
                var value = primitive.asBigDecimal(BigDecimal.ZERO);

                try {
                    var asLong = value.longValueExact();

                    if ((byte) asLong == asLong) {
                        visitor.writeByte(ctx, (byte) primitive.getValue());
                    } else if ((short) asLong == asLong) {
                        visitor.writeShort(ctx, (short) primitive.getValue());
                    } else if ((int) asLong == asLong) {
                        visitor.writeInt(ctx, (int) primitive.getValue());
                    } else {
                        visitor.writeLong(ctx, asLong);
                    }
                } catch (ArithmeticException bruh /* quite cringe java moment, why use an exception for this */) {
                    var asDouble = value.doubleValue();

                    if ((float) asDouble == asDouble) {
                        visitor.writeFloat(ctx, (float) primitive.getValue());
                    } else {
                        visitor.writeDouble(ctx, asDouble);
                    }
                }
            }
        } else if (element instanceof JsonArray array) {
            try (var sequence = visitor.sequence(ctx, Endec.<JsonElement>of(this::decodeValue, (ctx1, deserializer) -> null), array.size())) {
                array.forEach(sequence::element);
            }
        } else if (element instanceof JsonObject object) {
            try (var map = visitor.map(ctx, Endec.<JsonElement>of(this::decodeValue, (ctx1, deserializer) -> null), object.size())) {
                object.forEach(map::entry);
            }
        } else {
            throw new IllegalArgumentException("Non-standard, unrecognized JsonElement implementation cannot be decoded");
        }
    }

    // ---

    private class Sequence<V> implements Deserializer.Sequence<V> {

        private final SerializationContext ctx;
        private final Endec<V> valueEndec;
        private final Iterator<JsonElement> elements;
        private final int size;

        private Sequence(SerializationContext ctx, Endec<V> valueEndec, JsonArray elements) {
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
            return JanksonDeserializer.this.frame(
                    () -> element,
                    () -> this.valueEndec.decode(this.ctx, JanksonDeserializer.this)
            );
        }
    }

    private class Map<V> implements Deserializer.Map<V> {

        private final SerializationContext ctx;
        private final Endec<V> valueEndec;
        private final Iterator<java.util.Map.Entry<String, JsonElement>> entries;
        private final int size;

        private Map(SerializationContext ctx, Endec<V> valueEndec, JsonObject entries) {
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
            return JanksonDeserializer.this.frame(
                    entry::getValue,
                    () -> java.util.Map.entry(entry.getKey(), this.valueEndec.decode(this.ctx, JanksonDeserializer.this))
            );
        }
    }

    private class Struct implements Deserializer.Struct {

        private final JsonObject object;

        private Struct(JsonObject object) {
            this.object = object;
        }

        @Override
        public <F> @Nullable F field(String name, SerializationContext ctx, Endec<F> endec, @Nullable Supplier<F> defaultValueFactory) {
            var element = this.object.get(name);
            if (element == null) {
                if(defaultValueFactory == null) {
                    throw new IllegalStateException("Field '" + name + "' was missing from serialized data, but no default value was provided");
                }

                return defaultValueFactory.get();
            }
            return JanksonDeserializer.this.frame(
                    () -> element,
                    () -> endec.decode(ctx, JanksonDeserializer.this)
            );
        }
    }
}
