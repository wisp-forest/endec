package io.wispforest.endec.format.data;


import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationAttribute;
import io.wispforest.endec.util.VarUtils;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class DataInputDeserializer implements Deserializer<DataInput> {

    protected final DataInput input;

    protected DataInputDeserializer(DataInput input) {
        this.input = input;
    }

    public static DataInputDeserializer of(DataInput input) {
        return new DataInputDeserializer(input);
    }

    // ---

    @Override
    public Set<SerializationAttribute> attributes() {
        return Set.of();
    }

    // ---

    @Override
    public byte readByte() {
        try {
            return this.input.readByte();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public short readShort() {
        try {
            return this.input.readShort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int readInt() {
        try {
            return this.input.readInt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long readLong() {
        try {
            return this.input.readLong();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public float readFloat() {
        try {
            return this.input.readFloat();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double readDouble() {
        try {
            return this.input.readDouble();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        try {
            return this.input.readBoolean();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String readString() {
        try {
            return this.input.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] readBytes() {
        var result = new byte[this.readVarInt()];

        try {
            this.input.readFully(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    @Override
    public <V> Optional<V> readOptional(Endec<V> endec) {
        return this.readBoolean()
                ? Optional.of(endec.decode(this))
                : Optional.empty();
    }

    // ---

    @Override
    public <V> V tryRead(Function<Deserializer<DataInput>, V> reader) {
        throw new UnsupportedOperationException("As DataInput cannot be rewound, tryRead(...) cannot be supported");
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
            return this.valueEndec.decode(DataInputDeserializer.this);
        }

        @Override
        public <F> @Nullable F field(String name, Endec<F> endec) {
            return endec.decode(DataInputDeserializer.this);
        }

        @Override
        public <F> @Nullable F field(@Nullable String field, Endec<F> endec, @Nullable F defaultValue) {
            return endec.decode(DataInputDeserializer.this);
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
                    DataInputDeserializer.this.readString(),
                    this.valueEndec.decode(DataInputDeserializer.this)
            );
        }
    }
}
