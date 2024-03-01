package io.wispforest.endec.format.data;

import io.wispforest.endec.Endec;
import io.wispforest.endec.ExtraDataSerializer;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.util.VarUtils;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;

public class DataOutputSerializer<D extends DataOutput> extends ExtraDataSerializer<D> {

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
    public void writeByte(byte value) {
        this.write(() -> this.output.writeByte(value));
    }

    @Override
    public void writeShort(short value) {
        this.write(() -> this.output.writeShort(value));
    }

    @Override
    public void writeInt(int value) {
        this.write(() -> this.output.writeInt(value));
    }

    @Override
    public void writeLong(long value) {
        this.write(() -> this.output.writeLong(value));
    }

    @Override
    public void writeFloat(float value) {
        this.write(() -> this.output.writeFloat(value));
    }

    @Override
    public void writeDouble(double value) {
        this.write(() -> this.output.writeDouble(value));
    }

    // ---

    @Override
    public void writeVarInt(int value) {
        VarUtils.writeInt(value, this::writeByte);
    }

    @Override
    public void writeVarLong(long value) {
        VarUtils.writeLong(value, this::writeByte);
    }

    // ---

    @Override
    public void writeBoolean(boolean value) {
        this.write(() -> this.output.writeBoolean(value));
    }

    @Override
    public void writeString(String value) {
        this.write(() -> this.output.writeUTF(value));
    }

    @Override
    public void writeBytes(byte[] bytes) {
        this.write(() -> {
            this.writeVarInt(bytes.length);
            this.output.write(bytes);
        });
    }

    @Override
    public <V> void writeOptional(Endec<V> endec, Optional<V> optional) {
        this.writeBoolean(optional.isPresent());
        optional.ifPresent(value -> endec.encode(this, value));
    }

    // ---

    @Override
    public <V> Map<V> map(Endec<V> valueEndec, int size) {
        this.writeVarInt(size);
        return new Sequence<>(valueEndec);
    }

    @Override
    public <E> Serializer.Sequence<E> sequence(Endec<E> elementEndec, int size) {
        this.writeVarInt(size);
        return new Sequence<>(elementEndec);
    }

    @Override
    public Struct struct() {
        return new Sequence<>(null);
    }

    // ---

    @Override
    public D result() {
        return this.output;
    }

    // ---

    protected class Sequence<V> implements Serializer.Sequence<V>, Struct, Map<V> {

        protected final Endec<V> valueEndec;

        protected Sequence(Endec<V> valueEndec) {
            this.valueEndec = valueEndec;
        }

        @Override
        public void element(V element) {
            this.valueEndec.encode(DataOutputSerializer.this, element);
        }

        @Override
        public void entry(String key, V value) {
            DataOutputSerializer.this.writeString(key);
            this.valueEndec.encode(DataOutputSerializer.this, value);
        }

        @Override
        public <F> Struct field(String name, Endec<F> endec, F value) {
            endec.encode(DataOutputSerializer.this, value);
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
