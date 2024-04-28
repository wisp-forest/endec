package io.wispforest.endec.format.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.wispforest.endec.Endec;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.data.ExtraDataContext;
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
    public void writeByte(ExtraDataContext ctx, byte value) {
        this.buffer.writeByte(value);
    }

    @Override
    public void writeShort(ExtraDataContext ctx, short value) {
        this.buffer.writeShort(value);
    }

    @Override
    public void writeInt(ExtraDataContext ctx, int value) {
        this.buffer.writeInt(value);
    }

    @Override
    public void writeLong(ExtraDataContext ctx, long value) {
        this.buffer.writeLong(value);
    }

    @Override
    public void writeFloat(ExtraDataContext ctx, float value) {
        this.buffer.writeFloat(value);
    }

    @Override
    public void writeDouble(ExtraDataContext ctx, double value) {
        this.buffer.writeDouble(value);
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
        this.buffer.writeBoolean(value);
    }

    @Override
    public void writeString(ExtraDataContext ctx, String value) {
        this.writeVarInt(ctx, value.length());
        ByteBufUtil.writeUtf8(this.buffer, value);
    }

    @Override
    public void writeBytes(ExtraDataContext ctx, byte[] bytes) {
        this.writeVarInt(ctx, bytes.length);
        this.buffer.writeBytes(bytes);
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
    public B result() {
        return this.buffer;
    }

    // ---

    private class Sequence<V> implements Serializer.Sequence<V>, Struct, Map<V> {

        private final Endec<V> valueEndec;
        private final ExtraDataContext ctx;

        private Sequence(Endec<V> valueEndec, ExtraDataContext ctx) {
            this.valueEndec = valueEndec;
            this.ctx = ctx;
        }

        @Override
        public void element(V element) {
            this.valueEndec.encode(ByteBufSerializer.this, ctx, element);
        }

        @Override
        public void entry(String key, V value) {
            ByteBufSerializer.this.writeString(ctx, key);
            this.valueEndec.encode(ByteBufSerializer.this, ctx, value);
        }

        @Override
        public <F> Struct field(ExtraDataContext ctx, String name, Endec<F> endec, F value) {
            endec.encode(ByteBufSerializer.this, ctx, value);
            return this;
        }

        @Override
        public void end() {}
    }
}
