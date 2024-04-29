package io.wispforest.endec.format.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.wispforest.endec.Endec;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.data.SerializationContext;
import io.wispforest.endec.util.VarUtils;

import java.util.Optional;

public class ByteBufSerializer<B extends ByteBuf> implements Serializer<B> {

    private final B buffer;

    protected ByteBufSerializer(B buffer) {
        this.buffer = buffer;
    }

    public static <B extends ByteBuf> ByteBufSerializer<B> of(B buffer) {
        return new ByteBufSerializer<>(buffer);
    }

    // ---

    @Override
    public void writeByte(SerializationContext ctx, byte value) {
        this.buffer.writeByte(value);
    }

    @Override
    public void writeShort(SerializationContext ctx, short value) {
        this.buffer.writeShort(value);
    }

    @Override
    public void writeInt(SerializationContext ctx, int value) {
        this.buffer.writeInt(value);
    }

    @Override
    public void writeLong(SerializationContext ctx, long value) {
        this.buffer.writeLong(value);
    }

    @Override
    public void writeFloat(SerializationContext ctx, float value) {
        this.buffer.writeFloat(value);
    }

    @Override
    public void writeDouble(SerializationContext ctx, double value) {
        this.buffer.writeDouble(value);
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
        this.buffer.writeBoolean(value);
    }

    @Override
    public void writeString(SerializationContext ctx, String value) {
        this.writeVarInt(ctx, value.length());
        ByteBufUtil.writeUtf8(this.buffer, value);
    }

    @Override
    public void writeBytes(SerializationContext ctx, byte[] bytes) {
        this.writeVarInt(ctx, bytes.length);
        this.buffer.writeBytes(bytes);
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
    public B result() {
        return this.buffer;
    }

    // ---

    private class Sequence<V> implements Serializer.Sequence<V>, Struct, Map<V> {

        private final Endec<V> valueEndec;
        private final SerializationContext ctx;

        private Sequence(Endec<V> valueEndec, SerializationContext ctx) {
            this.valueEndec = valueEndec;
            this.ctx = ctx;
        }

        @Override
        public void element(V element) {
            this.valueEndec.encode(ctx, ByteBufSerializer.this, element);
        }

        @Override
        public void entry(String key, V value) {
            ByteBufSerializer.this.writeString(ctx, key);
            this.valueEndec.encode(ctx, ByteBufSerializer.this, value);
        }

        @Override
        public <F> Struct field(SerializationContext ctx, String name, Endec<F> endec, F value) {
            endec.encode(ctx, ByteBufSerializer.this, value);
            return this;
        }

        @Override
        public void end() {}
    }
}
