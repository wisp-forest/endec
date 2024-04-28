package io.wispforest.endec.format.bytebuf;

import io.netty.buffer.ByteBuf;
import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.data.ExtraDataContext;
import io.wispforest.endec.util.VarUtils;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.BiFunction;

public class ByteBufDeserializer implements Deserializer<ByteBuf> {

    private final ByteBuf buffer;

    protected ByteBufDeserializer(ByteBuf buffer) {
        this.buffer = buffer;
    }

    public static ByteBufDeserializer of(ByteBuf buffer) {
        return new ByteBufDeserializer(buffer);
    }

    // ---

    @Override
    public byte readByte(ExtraDataContext ctx) {
        return this.buffer.readByte();
    }

    @Override
    public short readShort(ExtraDataContext ctx) {
        return this.buffer.readShort();
    }

    @Override
    public int readInt(ExtraDataContext ctx) {
        return this.buffer.readInt();
    }

    @Override
    public long readLong(ExtraDataContext ctx) {
        return this.buffer.readLong();
    }

    @Override
    public float readFloat(ExtraDataContext ctx) {
        return this.buffer.readFloat();
    }

    @Override
    public double readDouble(ExtraDataContext ctx) {
        return this.buffer.readDouble();
    }

    // ---

    @Override
    public int readVarInt(ExtraDataContext ctx) {
        return VarUtils.readInt(() -> readByte(ctx));
    }

    @Override
    public long readVarLong(ExtraDataContext ctx) {
        return VarUtils.readLong(() -> readByte(ctx));
    }

    // ---

    @Override
    public boolean readBoolean(ExtraDataContext ctx) {
        return this.buffer.readBoolean();
    }

    @Override
    public String readString(ExtraDataContext ctx) {
        var sequenceLength = this.readVarInt(ctx);

        var string = this.buffer.toString(this.buffer.readerIndex(), sequenceLength, StandardCharsets.UTF_8);

        this.buffer.readerIndex(this.buffer.readerIndex() + sequenceLength);

        return string;
    }

    @Override
    public byte[] readBytes(ExtraDataContext ctx) {
        var array = new byte[this.readVarInt(ctx)];
        this.buffer.readBytes(array);

        return array;
    }

    @Override
    public <V> Optional<V> readOptional(ExtraDataContext ctx, Endec<V> endec) {
        return this.readBoolean(ctx)
                ? Optional.of(endec.decode(this, ctx))
                : Optional.empty();
    }

    // ---

    @Override
    public <V> V tryRead(BiFunction<Deserializer<ByteBuf>, ExtraDataContext, V> reader, ExtraDataContext ctx) {
        var prevReaderIdx = this.buffer.readerIndex();

        try {
            return reader.apply(this, ctx);
        } catch (Exception e) {
            this.buffer.readerIndex(prevReaderIdx);
            throw e;
        }
    }

    // ---

    @Override
    public <E> Deserializer.Sequence<E> sequence(ExtraDataContext ctx, Endec<E> elementEndec) {
        return new Sequence<>(elementEndec, this.readVarInt(ctx), ctx);
    }

    @Override
    public <V> Deserializer.Map<V> map(ExtraDataContext ctx, Endec<V> valueEndec) {
        return new Map<>(valueEndec, this.readVarInt(ctx), ctx);
    }

    @Override
    public Struct struct() {
        return new Sequence<>(null, 0, ExtraDataContext.of());
    }

    // ---

    private class Sequence<V> implements Deserializer.Sequence<V>, Struct {

        private final Endec<V> valueEndec;
        private final int size;

        private final ExtraDataContext ctx;

        private int index = 0;

        private Sequence(Endec<V> valueEndec, int size, ExtraDataContext ctx) {
            this.valueEndec = valueEndec;
            this.size = size;

            this.ctx = ctx;
        }

        @Override
        public int estimatedSize() {
            return this.size;
        }

        @Override
        public boolean hasNext() {
            return this.index < this.size;
        }

        @Override
        public V next() {
            this.index++;
            return this.valueEndec.decode(ByteBufDeserializer.this, ctx);
        }

        @Override
        public <F> @Nullable F field(ExtraDataContext ctx, String name, Endec<F> endec) {
            return this.field(ctx, name, endec, null);
        }

        @Override
        public <F> @Nullable F field(ExtraDataContext ctx, String name, Endec<F> endec, @Nullable F defaultValue) {
            return endec.decode(ByteBufDeserializer.this, ctx);
        }
    }

    private class Map<V> implements Deserializer.Map<V> {

        private final Endec<V> valueEndec;
        private final int size;

        private final ExtraDataContext ctx;

        private int index = 0;

        private Map(Endec<V> valueEndec, int size, ExtraDataContext ctx) {
            this.valueEndec = valueEndec;
            this.size = size;

            this.ctx = ctx;
        }

        @Override
        public int estimatedSize() {
            return this.size;
        }

        @Override
        public boolean hasNext() {
            return this.index < this.size;
        }

        @Override
        public java.util.Map.Entry<String, V> next() {
            this.index++;
            return java.util.Map.entry(
                    ByteBufDeserializer.this.readString(ctx),
                    this.valueEndec.decode(ByteBufDeserializer.this, ctx)
            );
        }
    }

}
