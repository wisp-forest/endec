package io.wispforest.endec.format.data;

import io.wispforest.endec.Endec;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.data.ExtraDataContext;
import io.wispforest.endec.util.VarUtils;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;

public class DataOutputSerializer<D extends DataOutput> implements Serializer<D> {

    protected final D output;

    protected DataOutputSerializer(D output) {
        this.output = output;
    }

    public static <D extends DataOutput> DataOutputSerializer<D> of(D output) {
        return new DataOutputSerializer<>(output);
    }

    protected void write(Writer writer) {
        try {
            writer.write();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ---

    @Override
    public void writeByte(ExtraDataContext ctx, byte value) {
        this.write(() -> this.output.writeByte(value));
    }

    @Override
    public void writeShort(ExtraDataContext ctx, short value) {
        this.write(() -> this.output.writeShort(value));
    }

    @Override
    public void writeInt(ExtraDataContext ctx, int value) {
        this.write(() -> this.output.writeInt(value));
    }

    @Override
    public void writeLong(ExtraDataContext ctx, long value) {
        this.write(() -> this.output.writeLong(value));
    }

    @Override
    public void writeFloat(ExtraDataContext ctx, float value) {
        this.write(() -> this.output.writeFloat(value));
    }

    @Override
    public void writeDouble(ExtraDataContext ctx, double value) {
        this.write(() -> this.output.writeDouble(value));
    }

    // ---

    @Override
    public void writeVarInt(ExtraDataContext ctx, int value) {
        VarUtils.writeInt(value, value1 -> writeByte(ctx, value1));
    }

    @Override
    public void writeVarLong(ExtraDataContext ctx, long value) {
        VarUtils.writeLong(value, value1 -> writeByte(ctx, value1));
    }

    // ---

    @Override
    public void writeBoolean(ExtraDataContext ctx, boolean value) {
        this.write(() -> this.output.writeBoolean(value));
    }

    @Override
    public void writeString(ExtraDataContext ctx, String value) {
        this.write(() -> this.output.writeUTF(value));
    }

    @Override
    public void writeBytes(ExtraDataContext ctx, byte[] bytes) {
        this.write(() -> {
            this.writeVarInt(ctx, bytes.length);
            this.output.write(bytes);
        });
    }

    @Override
    public <V> void writeOptional(ExtraDataContext ctx, Endec<V> endec, Optional<V> optional) {
        this.writeBoolean(ctx, optional.isPresent());
        optional.ifPresent(value -> endec.encode(this, ctx, value));
    }

    // ---

    @Override
    public <V> Map<V> map(ExtraDataContext ctx, Endec<V> valueEndec, int size) {
        this.writeVarInt(ctx, size);
        return new Sequence<>(valueEndec, ctx);
    }

    @Override
    public <E> Serializer.Sequence<E> sequence(ExtraDataContext ctx, Endec<E> elementEndec, int size) {
        this.writeVarInt(ctx, size);
        return new Sequence<>(elementEndec, ctx);
    }

    @Override
    public Struct struct() {
        return new Sequence<>(null, ExtraDataContext.of());
    }

    // ---

    @Override
    public D result() {
        return this.output;
    }

    // ---

    protected class Sequence<V> implements Serializer.Sequence<V>, Struct, Map<V> {

        protected final Endec<V> valueEndec;

        private final ExtraDataContext ctx;

        protected Sequence(Endec<V> valueEndec, ExtraDataContext ctx) {
            this.valueEndec = valueEndec;
            this.ctx = ctx;
        }

        @Override
        public void element(V element) {
            this.valueEndec.encode(DataOutputSerializer.this, ctx, element);
        }

        @Override
        public void entry(String key, V value) {
            DataOutputSerializer.this.writeString(ctx, key);
            this.valueEndec.encode(DataOutputSerializer.this, ctx, value);
        }

        @Override
        public <F> Struct field(ExtraDataContext ctx, String name, Endec<F> endec, F value) {
            endec.encode(DataOutputSerializer.this, ctx, value);
            return this;
        }

        @Override
        public void end() {}
    }

    @FunctionalInterface
    protected interface Writer {
        void write() throws IOException;
    }
}
