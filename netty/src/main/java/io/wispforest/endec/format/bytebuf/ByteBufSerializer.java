package io.wispforest.endec.format.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.wispforest.endec.Endec;
import io.wispforest.endec.ExtraDataSerializer;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.util.VarUtils;

import java.util.Optional;

public class ByteBufSerializer<B extends ByteBuf> extends ExtraDataSerializer<B> {

    private final B buffer;

    protected ByteBufSerializer(B buffer) {
        this.buffer = buffer;
    }

    public static <B extends ByteBuf> ByteBufSerializer<B> of(B buffer) {
        return new ByteBufSerializer<>(buffer);
    }

    // ---

    @Override
    public void writeByte(byte value) {
        this.buffer.writeByte(value);
    }

    @Override
    public void writeShort(short value) {
        this.buffer.writeShort(value);
    }

    @Override
    public void writeInt(int value) {
        this.buffer.writeInt(value);
    }

    @Override
    public void writeLong(long value) {
        this.buffer.writeLong(value);
    }

    @Override
    public void writeFloat(float value) {
        this.buffer.writeFloat(value);
    }

    @Override
    public void writeDouble(double value) {
        this.buffer.writeDouble(value);
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
        this.buffer.writeBoolean(value);
    }

    @Override
    public void writeString(String value) {
        this.writeVarInt(value.length());
        ByteBufUtil.writeUtf8(this.buffer, value);
    }

    @Override
    public void writeBytes(byte[] bytes) {
        this.writeVarInt(bytes.length);
        this.buffer.writeBytes(bytes);
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
    public B result() {
        return this.buffer;
    }

    // ---

    private class Sequence<V> implements Serializer.Sequence<V>, Struct, Map<V> {

        private final Endec<V> valueEndec;

        private Sequence(Endec<V> valueEndec) {
            this.valueEndec = valueEndec;
        }

        @Override
        public void element(V element) {
            this.valueEndec.encode(ByteBufSerializer.this, element);
        }

        @Override
        public void entry(String key, V value) {
            ByteBufSerializer.this.writeString(key);
            this.valueEndec.encode(ByteBufSerializer.this, value);
        }

        @Override
        public <F> Struct field(String name, Endec<F> endec, F value) {
            endec.encode(ByteBufSerializer.this, value);
            return this;
        }

        @Override
        public void end() {}
    }
}
