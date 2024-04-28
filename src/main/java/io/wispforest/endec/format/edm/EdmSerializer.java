package io.wispforest.endec.format.edm;

import io.wispforest.endec.*;
import io.wispforest.endec.data.DataTokens;
import io.wispforest.endec.data.ExtraDataContext;
import io.wispforest.endec.util.MutableHolder;
import io.wispforest.endec.util.RecursiveSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EdmSerializer extends RecursiveSerializer<EdmElement<?>> implements SelfDescribedSerializer<EdmElement<?>> {

    protected EdmSerializer() {
        super(null);
    }

    public static EdmSerializer of() {
        return new EdmSerializer();
    }

    // ---

    @Override
    public void writeByte(ExtraDataContext ctx, byte value) {
        this.consume(EdmElement.wrapByte(value));
    }

    @Override
    public void writeShort(ExtraDataContext ctx, short value) {
        this.consume(EdmElement.wrapShort(value));
    }

    @Override
    public void writeInt(ExtraDataContext ctx, int value) {
        this.consume(EdmElement.wrapInt(value));
    }

    @Override
    public void writeLong(ExtraDataContext ctx, long value) {
        this.consume(EdmElement.wrapLong(value));
    }

    // ---

    @Override
    public void writeFloat(ExtraDataContext ctx, float value) {
        this.consume(EdmElement.wrapFloat(value));
    }

    @Override
    public void writeDouble(ExtraDataContext ctx, double value) {
        this.consume(EdmElement.wrapDouble(value));
    }

    // ---

    @Override
    public void writeVarInt(ExtraDataContext ctx, int value) {
        this.consume(EdmElement.wrapInt(value));
    }

    @Override
    public void writeVarLong(ExtraDataContext ctx, long value) {
        this.consume(EdmElement.wrapLong(value));
    }

    // ---

    @Override
    public void writeBoolean(ExtraDataContext ctx, boolean value) {
        this.consume(EdmElement.wrapBoolean(value));
    }

    @Override
    public void writeString(ExtraDataContext ctx, String value) {
        this.consume(EdmElement.wrapString(value));
    }

    @Override
    public void writeBytes(ExtraDataContext ctx, byte[] bytes) {
        this.consume(EdmElement.wrapBytes(bytes));
    }

    @Override
    public <V> void writeOptional(ExtraDataContext ctx, Endec<V> endec, Optional<V> optional) {
        var holder = new MutableHolder<@Nullable EdmElement<?>>();

        this.frame(encoded -> {
            optional.ifPresent(v -> endec.encode(this, ctx, v));

            holder.setValue(encoded.get());
        }, false);

        this.consume(EdmElement.wrapOptional(Optional.ofNullable(holder.getValue())));
    }

    // ---

    @Override
    public <E> Serializer.Sequence<E> sequence(ExtraDataContext ctx, Endec<E> elementEndec, int size) {
        return new Sequence<>(elementEndec, ctx);
    }

    @Override
    public <V> Serializer.Map<V> map(ExtraDataContext ctx, Endec<V> valueEndec, int size) {
        return new Map<>(valueEndec, ctx);
    }

    @Override
    public Serializer.Struct struct() {
        return new Struct();
    }

    // ---

    private class Sequence<V> implements Serializer.Sequence<V> {

        private final Endec<V> elementEndec;
        private final ExtraDataContext ctx;

        private final List<EdmElement<?>> result;

        private Sequence(Endec<V> elementEndec, ExtraDataContext ctx) {
            this.elementEndec = elementEndec;
            this.ctx = ctx;
            this.result = new ArrayList<>();
        }

        @Override
        public void element(V element) {
            EdmSerializer.this.frame(encoded -> {
                this.elementEndec.encode(EdmSerializer.this, ctx, element);
                this.result.add(encoded.require("sequence element"));
            }, false);
        }

        @Override
        public void end() {
            EdmSerializer.this.consume(EdmElement.wrapSequence(this.result));
        }
    }

    private class Map<V> implements Serializer.Map<V> {

        private final Endec<V> valueEndec;
        private final ExtraDataContext ctx;

        private final java.util.Map<String, EdmElement<?>> result;

        private Map(Endec<V> valueEndec, ExtraDataContext ctx) {
            this.valueEndec = valueEndec;
            this.ctx = ctx;

            this.result = new HashMap<>();
        }

        @Override
        public void entry(String key, V value) {
            EdmSerializer.this.frame(encoded -> {
                this.valueEndec.encode(EdmSerializer.this, ctx, value);
                this.result.put(key, encoded.require("map value"));
            }, false);
        }

        @Override
        public void end() {
            EdmSerializer.this.consume(EdmElement.wrapMap(this.result));
        }
    }

    private class Struct implements Serializer.Struct {

        private final java.util.Map<String, EdmElement<?>> result;

        private Struct() {
            this.result = new HashMap<>();
        }

        @Override
        public <F> Serializer.Struct field(ExtraDataContext ctx, String name, Endec<F> endec, F value) {
            EdmSerializer.this.frame(encoded -> {
                endec.encode(EdmSerializer.this, ctx, value);
                this.result.put(name, encoded.require("struct field"));
            }, true);

            return this;
        }

        @Override
        public void end() {
            EdmSerializer.this.consume(EdmElement.wrapMap(this.result));
        }
    }
}
