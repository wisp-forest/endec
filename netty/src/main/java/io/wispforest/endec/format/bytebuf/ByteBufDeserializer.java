package io.wispforest.endec.format.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationAttribute;
import io.wispforest.endec.util.VarUtils;

import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

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
    public Set<SerializationAttribute> attributes() {
        return Set.of();
    }

    // ---

    @Override
    public byte readByte() {
        return this.buffer.readByte();
    }

    @Override
    public short readShort() {
        return this.buffer.readShort();
    }

    @Override
    public int readInt() {
        return this.buffer.readInt();
    }

    @Override
    public long readLong() {
        return this.buffer.readLong();
    }

    @Override
    public float readFloat() {
        return this.buffer.readFloat();
    }

    @Override
    public double readDouble() {
        return this.buffer.readDouble();
    }

    // ---

    @Override
    public int readVarInt() {
        return VarUtils.readInt(this::readByte);
    }

    @Override
    public long readVarLong() {
        return VarUtils.readLong(this::readByte);
    }

    // ---

    @Override
    public boolean readBoolean() {
        return this.buffer.readBoolean();
    }

    @Override
    public String readString() {
        var sequenceLength = this.readVarInt();

        var string = this.buffer.toString(this.buffer.readerIndex(), sequenceLength, StandardCharsets.UTF_8);

        this.buffer.readerIndex(this.buffer.readerIndex() + sequenceLength);

        return string;
    }

    @Override
    public byte[] readBytes() {
        var array = new byte[this.readVarInt()];
        this.buffer.readBytes(array);

        return array;
    }

    @Override
    public <V> Optional<V> readOptional(Endec<V> endec) {
        return this.readBoolean()
                ? Optional.of(endec.decode(this))
                : Optional.empty();
    }

    // ---

    @Override
    public <V> V tryRead(Function<Deserializer<ByteBuf>, V> reader) {
        var prevReaderIdx = this.buffer.readerIndex();

        try {
            return reader.apply(this);
        } catch (Exception e) {
            this.buffer.readerIndex(prevReaderIdx);
            throw e;
        }
    }

    // ---

    @Override
    public <E> Deserializer.Sequence<E> sequence(Endec<E> elementEndec) {
        return new Sequence<>(elementEndec, this.readVarInt());
    }

    @Override
    public <V> Deserializer.Map<V> map(Endec<V> valueEndec) {
        return new Map<>(valueEndec, this.readVarInt());
    }

    @Override
    public Struct struct() {
        return new Sequence<>(null, 0);
    }

    // ---

    private class Sequence<V> implements Deserializer.Sequence<V>, Struct {

        private final Endec<V> valueEndec;
        private final int size;

        private int index = 0;

        private Sequence(Endec<V> valueEndec, int size) {
            this.valueEndec = valueEndec;
            this.size = size;
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
            return this.valueEndec.decode(ByteBufDeserializer.this);
        }

        @Override
        public <F> @Nullable F field(String name, Endec<F> endec) {
            return this.field(name, endec, null);
        }

        @Override
        public <F> @Nullable F field(String name, Endec<F> endec, @Nullable F defaultValue) {
            return endec.decode(ByteBufDeserializer.this);
        }
    }

    private class Map<V> implements Deserializer.Map<V> {

        private final Endec<V> valueEndec;
        private final int size;

        private int index = 0;

        private Map(Endec<V> valueEndec, int size) {
            this.valueEndec = valueEndec;
            this.size = size;
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
                    ByteBufDeserializer.this.readString(),
                    this.valueEndec.decode(ByteBufDeserializer.this)
            );
        }
    }

}
