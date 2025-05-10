package io.wispforest.endec.format.edm;

import io.wispforest.endec.Endec;
import io.wispforest.endec.SelfDescribedSerializer;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.util.RecursiveSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class EdmSerializer extends RecursiveSerializer<EdmElement<?>> implements SelfDescribedSerializer<EdmElement<?>> {

    protected EdmSerializer() {
        super(null);
    }

    public static EdmSerializer of() {
        return new EdmSerializer();
    }

    // ---

    @Override
    public void writeByte(SerializationContext ctx, byte value) {
        this.consume(EdmElement.i8(value));
    }

    @Override
    public void writeShort(SerializationContext ctx, short value) {
        this.consume(EdmElement.i16(value));
    }

    @Override
    public void writeInt(SerializationContext ctx, int value) {
        this.consume(EdmElement.i32(value));
    }

    @Override
    public void writeLong(SerializationContext ctx, long value) {
        this.consume(EdmElement.i64(value));
    }

    // ---

    @Override
    public void writeFloat(SerializationContext ctx, float value) {
        this.consume(EdmElement.f32(value));
    }

    @Override
    public void writeDouble(SerializationContext ctx, double value) {
        this.consume(EdmElement.f64(value));
    }

    // ---

    @Override
    public void writeVarInt(SerializationContext ctx, int value) {
        this.consume(EdmElement.i32(value));
    }

    @Override
    public void writeVarLong(SerializationContext ctx, long value) {
        this.consume(EdmElement.i64(value));
    }

    // ---

    @Override
    public void writeBoolean(SerializationContext ctx, boolean value) {
        this.consume(EdmElement.bool(value));
    }

    @Override
    public void writeString(SerializationContext ctx, String value) {
        this.consume(EdmElement.string(value));
    }

    @Override
    public void writeBytes(SerializationContext ctx, byte[] bytes) {
        this.consume(EdmElement.bytes(bytes));
    }

    @Override
    public <V> void writeOptional(SerializationContext ctx, Endec<V> endec, Optional<V> optional) {
        var result = new EdmElement<?>[1];
        this.frame(encoded -> {
            optional.ifPresent(v -> endec.encode(ctx, this, v));
            result[0] = encoded.value();
        });

        this.consume(EdmElement.optional(Optional.ofNullable(result[0])));
    }

    // ---

    @Override
    public <E> Serializer.Sequence<E> sequence(SerializationContext ctx, Endec<E> elementEndec, int size) {
        return new Sequence<>(elementEndec, ctx);
    }

    @Override
    public <V> Serializer.Map<V> map(SerializationContext ctx, Endec<V> valueEndec, int size) {
        return new Map<>(valueEndec, ctx);
    }

    @Override
    public Serializer.Struct struct() {
        return new Struct();
    }

    // ---

    private class Sequence<V> implements Serializer.Sequence<V> {

        private final Endec<V> elementEndec;
        private final SerializationContext ctx;

        private final List<EdmElement<?>> result;

        private Sequence(Endec<V> elementEndec, SerializationContext ctx) {
            this.elementEndec = elementEndec;
            this.ctx = ctx;
            this.result = new ArrayList<>();
        }

        @Override
        public void element(V element) {
            EdmSerializer.this.frame(encoded -> {
                this.elementEndec.encode(ctx, EdmSerializer.this, element);
                this.result.add(encoded.require("sequence element"));
            });
        }

        @Override
        public void end() {
            EdmSerializer.this.consume(EdmElement.sequence(this.result));
        }
    }

    private class Map<V> implements Serializer.Map<V> {

        private final Endec<V> valueEndec;
        private final SerializationContext ctx;

        private final java.util.Map<String, EdmElement<?>> result;

        private Map(Endec<V> valueEndec, SerializationContext ctx) {
            this.valueEndec = valueEndec;
            this.ctx = ctx;

            this.result = new HashMap<>();
        }

        @Override
        public void entry(String key, V value) {
            EdmSerializer.this.frame(encoded -> {
                this.valueEndec.encode(ctx, EdmSerializer.this, value);
                this.result.put(key, encoded.require("map value"));
            });
        }

        @Override
        public void end() {
            EdmSerializer.this.consume(EdmElement.consumeMap(this.result));
        }
    }

    private class Struct implements Serializer.Struct {

        private final java.util.Map<String, EdmElement<?>> result;

        private Struct() {
            this.result = new HashMap<>();
        }

        @Override
        public <F> Serializer.Struct field(String name, SerializationContext ctx, Endec<F> endec, F value, boolean mayOmit) {
            EdmSerializer.this.frame(encoded -> {
                endec.encode(ctx, EdmSerializer.this, value);

                var element = encoded.require("struct field");

                if (mayOmit && element.equals(EdmElement.EMPTY)) return;

                this.result.put(name, element);
            });

            return this;
        }

        @Override
        public void end() {
            EdmSerializer.this.consume(EdmElement.consumeMap(this.result));
        }
    }
}
