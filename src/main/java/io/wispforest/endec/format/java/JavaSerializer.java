package io.wispforest.endec.format.java;

import io.wispforest.endec.Endec;
import io.wispforest.endec.SelfDescribedSerializer;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.impl.CommentAttribute;
import io.wispforest.endec.util.RecursiveSerializer;

import java.util.*;

public class JavaSerializer extends RecursiveSerializer<Object> implements SelfDescribedSerializer<Object> {

    private final java.util.Map<Object, java.util.Map<String, String>> commentLookupMap = new HashMap<>();

    protected JavaSerializer() {
        super(null);
    }

    public static JavaSerializer of() {
        return new JavaSerializer();
    }

    public java.util.Map<String, String> getComments(Object object) {
        return commentLookupMap.containsKey(object)
                ? java.util.Map.of()
                : Collections.unmodifiableMap(commentLookupMap.get(object));
    }

    // ---

    @Override
    public void writeByte(SerializationContext ctx, byte value) {
        this.consume(value);
    }

    @Override
    public void writeShort(SerializationContext ctx, short value) {
        this.consume(value);
    }

    @Override
    public void writeInt(SerializationContext ctx, int value) {
        this.consume(value);
    }

    @Override
    public void writeLong(SerializationContext ctx, long value) {
        this.consume(value);
    }

    // ---

    @Override
    public void writeFloat(SerializationContext ctx, float value) {
        this.consume(value);
    }

    @Override
    public void writeDouble(SerializationContext ctx, double value) {
        this.consume(value);
    }

    // ---

    @Override
    public void writeVarInt(SerializationContext ctx, int value) {
        this.consume(value);
    }

    @Override
    public void writeVarLong(SerializationContext ctx, long value) {
        this.consume(value);
    }

    // ---

    @Override
    public void writeBoolean(SerializationContext ctx, boolean value) {
        this.consume(value);
    }

    @Override
    public void writeString(SerializationContext ctx, String value) {
        this.consume(value);
    }

    @Override
    public void writeBytes(SerializationContext ctx, byte[] bytes) {
        this.consume(bytes);
    }

    @Override
    public <V> void writeOptional(SerializationContext ctx, Endec<V> endec, Optional<V> optional) {
        var result = new Object[1];
        this.frame(encoded -> {
            optional.ifPresent(v -> endec.encode(ctx, this, v));
            result[0] = encoded.value();
        });

        this.consume(Optional.ofNullable(result[0]));
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

        private final List<Object> result;

        private Sequence(Endec<V> elementEndec, SerializationContext ctx) {
            this.elementEndec = elementEndec;
            this.ctx = ctx;
            this.result = new ArrayList<>();
        }

        @Override
        public void element(V element) {
            JavaSerializer.this.frame(encoded -> {
                this.elementEndec.encode(ctx, JavaSerializer.this, element);
                this.result.add(encoded.require("sequence element"));
            });
        }

        @Override
        public void end() {
            JavaSerializer.this.consume(this.result);
        }
    }

    private class Map<V> implements Serializer.Map<V> {

        private final Endec<V> valueEndec;
        private final SerializationContext ctx;

        private final java.util.Map<String, Object> result;

        private Map(Endec<V> valueEndec, SerializationContext ctx) {
            this.valueEndec = valueEndec;
            this.ctx = ctx;

            this.result = new HashMap<>();
        }

        @Override
        public void entry(String key, V value) {
            JavaSerializer.this.frame(encoded -> {
                this.valueEndec.encode(ctx, JavaSerializer.this, value);
                this.result.put(key, encoded.require("map value"));
            });
        }

        @Override
        public void end() {
            JavaSerializer.this.consume(this.result);
        }
    }

    private class Struct implements Serializer.Struct {

        private final java.util.Map<String, Object> result;

        private Struct() {
            this.result = new HashMap<>();
        }

        @Override
        public <F> Serializer.Struct field(String name, SerializationContext ctx, Endec<F> endec, F value, boolean mayOmit) {
            JavaSerializer.this.frame(encoded -> {
                endec.encode(ctx, JavaSerializer.this, value);

                var element = encoded.require("struct field");

                if (mayOmit && element.equals(Optional.empty())) return;

                this.result.put(name, element);

                CommentAttribute.addComment(ctx, comment -> {
                    JavaSerializer.this.commentLookupMap
                            .computeIfAbsent(this.result, object -> new HashMap<>())
                            .put(name, comment);
                });

            });

            return this;
        }

        @Override
        public void end() {
            JavaSerializer.this.consume(this.result);
        }
    }
}
