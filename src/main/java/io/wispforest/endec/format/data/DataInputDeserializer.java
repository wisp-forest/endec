package io.wispforest.endec.format.data;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.util.VarInts;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

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
    public byte readByte(SerializationContext ctx) {
        try {
            return this.input.readByte();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public short readShort(SerializationContext ctx) {
        try {
            return this.input.readShort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int readInt(SerializationContext ctx) {
        try {
            return this.input.readInt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long readLong(SerializationContext ctx) {
        try {
            return this.input.readLong();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public float readFloat(SerializationContext ctx) {
        try {
            return this.input.readFloat();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double readDouble(SerializationContext ctx) {
        try {
            return this.input.readDouble();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ---

    @Override
    public int readVarInt(SerializationContext ctx) {
        return VarInts.readInt(() -> this.readByte(ctx));
    }

    @Override
    public long readVarLong(SerializationContext ctx) {
        return VarInts.readLong(() -> this.readByte(ctx));
    }

    // ---

    @Override
    public boolean readBoolean(SerializationContext ctx) {
        try {
            return this.input.readBoolean();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String readString(SerializationContext ctx) {
        try {
            return this.input.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] readBytes(SerializationContext ctx) {
        var result = new byte[this.readVarInt(ctx)];

        try {
            this.input.readFully(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    @Override
    public <V> Optional<V> readOptional(SerializationContext ctx, Endec<V> endec) {
        return this.readBoolean(ctx)
                ? Optional.of(endec.decode(ctx, this))
                : Optional.empty();
    }

    // ---

    @Override
    public <V> V tryRead(Function<Deserializer<DataInput>, V> reader) {
        throw new UnsupportedOperationException("As DataInput cannot be rewound, tryRead(...) cannot be supported");
    }

    // ---

    @Override
    public <E> Deserializer.Sequence<E> sequence(SerializationContext ctx, Endec<E> elementEndec) {
        return new Sequence<>(ctx, elementEndec, this.readVarInt(ctx));
    }

    @Override
    public <V> Deserializer.Map<V> map(SerializationContext ctx, Endec<V> valueEndec) {
        return new Map<>(ctx, valueEndec, this.readVarInt(ctx));
    }

    @Override
    public Struct struct() {
        return new Sequence<>(null, null, 0);
    }

    // ---

    private class Sequence<V> implements Deserializer.Sequence<V>, Struct {

        private final SerializationContext ctx;
        private final Endec<V> valueEndec;
        private final int size;

        private int index = 0;

        private Sequence(SerializationContext ctx, Endec<V> valueEndec, int size) {
            this.ctx = ctx;
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
            return this.valueEndec.decode(this.ctx, DataInputDeserializer.this);
        }

        @Override
        public <F> @Nullable F field(String name, SerializationContext ctx, Endec<F> endec, @Nullable Supplier<F> defaultValueFactory) {
            return endec.decode(ctx, DataInputDeserializer.this);
        }
    }

    private class Map<V> implements Deserializer.Map<V> {

        private final SerializationContext ctx;
        private final Endec<V> valueEndec;
        private final int size;

        private int index = 0;

        private Map(SerializationContext ctx, Endec<V> valueEndec, int size) {
            this.ctx = ctx;
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
                    DataInputDeserializer.this.readString(this.ctx),
                    this.valueEndec.decode(this.ctx, DataInputDeserializer.this)
            );
        }
    }
}
