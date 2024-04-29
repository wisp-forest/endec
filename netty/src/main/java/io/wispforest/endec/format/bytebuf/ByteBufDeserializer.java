package io.wispforest.endec.format.bytebuf;

import io.netty.buffer.ByteBuf;
import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.data.SerializationContext;
import io.wispforest.endec.util.Decoder;
import io.wispforest.endec.util.VarUtils;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

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
    public byte readByte(SerializationContext ctx) {
        return this.buffer.readByte();
    }

    @Override
    public short readShort(SerializationContext ctx) {
        return this.buffer.readShort();
    }

    @Override
    public int readInt(SerializationContext ctx) {
        return this.buffer.readInt();
    }

    @Override
    public long readLong(SerializationContext ctx) {
        return this.buffer.readLong();
    }

    @Override
    public float readFloat(SerializationContext ctx) {
        return this.buffer.readFloat();
    }

    @Override
    public double readDouble(SerializationContext ctx) {
        return this.buffer.readDouble();
    }

    // ---

    @Override
    public int readVarInt(SerializationContext ctx) {
        return VarUtils.readInt(() -> readByte(ctx));
    }

    @Override
    public long readVarLong(SerializationContext ctx) {
        return VarUtils.readLong(() -> readByte(ctx));
    }

    // ---

    @Override
    public boolean readBoolean(SerializationContext ctx) {
        return this.buffer.readBoolean();
    }

    @Override
    public String readString(SerializationContext ctx) {
        var sequenceLength = this.readVarInt(ctx);

        var string = this.buffer.toString(this.buffer.readerIndex(), sequenceLength, StandardCharsets.UTF_8);

        this.buffer.readerIndex(this.buffer.readerIndex() + sequenceLength);

        return string;
    }

    @Override
    public byte[] readBytes(SerializationContext ctx) {
        var array = new byte[this.readVarInt(ctx)];
        this.buffer.readBytes(array);

        return array;
    }

    @Override
    public <V> Optional<V> readOptional(SerializationContext ctx, Endec<V> endec) {
        return this.readBoolean(ctx)
                ? Optional.of(endec.decode(ctx, this))
                : Optional.empty();
    }

    // ---

    @Override
    public <V> V tryRead(SerializationContext ctx, Decoder<V> reader) {
        var prevReaderIdx = this.buffer.readerIndex();

        try {
            return reader.decode(ctx, this);
        } catch (Exception e) {
            this.buffer.readerIndex(prevReaderIdx);
            throw e;
        }
    }

    // ---

    @Override
    public <E> Deserializer.Sequence<E> sequence(SerializationContext ctx, Endec<E> elementEndec) {
        return new Sequence<>(elementEndec, this.readVarInt(ctx), ctx);
    }

    @Override
    public <V> Deserializer.Map<V> map(SerializationContext ctx, Endec<V> valueEndec) {
        return new Map<>(valueEndec, this.readVarInt(ctx), ctx);
    }

    @Override
    public Struct struct() {
        return new Sequence<>(null, 0, SerializationContext.of());
    }

    // ---

    private class Sequence<V> implements Deserializer.Sequence<V>, Struct {

        private final Endec<V> valueEndec;
        private final int size;

        private final SerializationContext ctx;

        private int index = 0;

        private Sequence(Endec<V> valueEndec, int size, SerializationContext ctx) {
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
            return this.valueEndec.decode(ctx, ByteBufDeserializer.this);
        }

        @Override
        public <F> @Nullable F field(SerializationContext ctx, String name, Endec<F> endec) {
            return this.field(ctx, name, endec, null);
        }

        @Override
        public <F> @Nullable F field(SerializationContext ctx, String name, Endec<F> endec, @Nullable F defaultValue) {
            return endec.decode(ctx, ByteBufDeserializer.this);
        }
    }

    private class Map<V> implements Deserializer.Map<V> {

        private final Endec<V> valueEndec;
        private final int size;

        private final SerializationContext ctx;

        private int index = 0;

        private Map(Endec<V> valueEndec, int size, SerializationContext ctx) {
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
                    this.valueEndec.decode(ctx, ByteBufDeserializer.this)
            );
        }
    }

}
