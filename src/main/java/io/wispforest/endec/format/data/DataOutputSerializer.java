package io.wispforest.endec.format.data;

import io.wispforest.endec.Endec;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.data.SerializationContext;
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
    public void writeByte(SerializationContext ctx, byte value) {
        this.write(() -> this.output.writeByte(value));
    }

    @Override
    public void writeShort(SerializationContext ctx, short value) {
        this.write(() -> this.output.writeShort(value));
    }

    @Override
    public void writeInt(SerializationContext ctx, int value) {
        this.write(() -> this.output.writeInt(value));
    }

    @Override
    public void writeLong(SerializationContext ctx, long value) {
        this.write(() -> this.output.writeLong(value));
    }

    @Override
    public void writeFloat(SerializationContext ctx, float value) {
        this.write(() -> this.output.writeFloat(value));
    }

    @Override
    public void writeDouble(SerializationContext ctx, double value) {
        this.write(() -> this.output.writeDouble(value));
    }

    // ---

    @Override
    public void writeVarInt(SerializationContext ctx, int value) {
        VarUtils.writeInt(value, value1 -> writeByte(ctx, value1));
    }

    @Override
    public void writeVarLong(SerializationContext ctx, long value) {
        VarUtils.writeLong(value, value1 -> writeByte(ctx, value1));
    }

    // ---

    @Override
    public void writeBoolean(SerializationContext ctx, boolean value) {
        this.write(() -> this.output.writeBoolean(value));
    }

    @Override
    public void writeString(SerializationContext ctx, String value) {
        this.write(() -> this.output.writeUTF(value));
    }

    @Override
    public void writeBytes(SerializationContext ctx, byte[] bytes) {
        this.write(() -> {
            this.writeVarInt(ctx, bytes.length);
            this.output.write(bytes);
        });
    }

    @Override
    public <V> void writeOptional(SerializationContext ctx, Endec<V> endec, Optional<V> optional) {
        this.writeBoolean(ctx, optional.isPresent());
        optional.ifPresent(value -> endec.encode(ctx, this, value));
    }

    // ---

    @Override
    public <V> Map<V> map(SerializationContext ctx, Endec<V> valueEndec, int size) {
        this.writeVarInt(ctx, size);
        return new Sequence<>(valueEndec, ctx);
    }

    @Override
    public <E> Serializer.Sequence<E> sequence(SerializationContext ctx, Endec<E> elementEndec, int size) {
        this.writeVarInt(ctx, size);
        return new Sequence<>(elementEndec, ctx);
    }

    @Override
    public Struct struct() {
        return new Sequence<>(null, SerializationContext.of());
    }

    // ---

    @Override
    public D result() {
        return this.output;
    }

    // ---

    protected class Sequence<V> implements Serializer.Sequence<V>, Struct, Map<V> {

        protected final Endec<V> valueEndec;

        private final SerializationContext ctx;

        protected Sequence(Endec<V> valueEndec, SerializationContext ctx) {
            this.valueEndec = valueEndec;
            this.ctx = ctx;
        }

        @Override
        public void element(V element) {
            this.valueEndec.encode(ctx, DataOutputSerializer.this, element);
        }

        @Override
        public void entry(String key, V value) {
            DataOutputSerializer.this.writeString(ctx, key);
            this.valueEndec.encode(ctx, DataOutputSerializer.this, value);
        }

        @Override
        public <F> Struct field(SerializationContext ctx, String name, Endec<F> endec, F value) {
            endec.encode(ctx, DataOutputSerializer.this, value);
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
