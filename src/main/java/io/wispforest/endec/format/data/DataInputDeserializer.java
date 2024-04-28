package io.wispforest.endec.format.data;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.data.ExtraDataContext;
import io.wispforest.endec.util.VarUtils;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.IOException;
import java.util.Optional;
import java.util.function.BiFunction;

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
    public byte readByte(ExtraDataContext ctx) {
        try {
            return this.input.readByte();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public short readShort(ExtraDataContext ctx) {
        try {
            return this.input.readShort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int readInt(ExtraDataContext ctx) {
        try {
            return this.input.readInt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long readLong(ExtraDataContext ctx) {
        try {
            return this.input.readLong();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public float readFloat(ExtraDataContext ctx) {
        try {
            return this.input.readFloat();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double readDouble(ExtraDataContext ctx) {
        try {
            return this.input.readDouble();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        try {
            return this.input.readBoolean();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String readString(ExtraDataContext ctx) {
        try {
            return this.input.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] readBytes(ExtraDataContext ctx) {
        var result = new byte[this.readVarInt(ctx)];

        try {
            this.input.readFully(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    @Override
    public <V> Optional<V> readOptional(ExtraDataContext ctx, Endec<V> endec) {
        return this.readBoolean(ctx)
                ? Optional.of(endec.decode(this, ctx))
                : Optional.empty();
    }

    // ---

    @Override
    public <V> V tryRead(BiFunction<Deserializer<DataInput>, ExtraDataContext, V> reader, ExtraDataContext ctx) {
        throw new UnsupportedOperationException("As DataInput cannot be rewound, tryRead(...) cannot be supported");
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
        private final ExtraDataContext ctx;

        private final int size;

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
            return this.valueEndec.decode(DataInputDeserializer.this, ctx);
        }

        @Override
        public <F> @Nullable F field(ExtraDataContext ctx, String name, Endec<F> endec) {
            return endec.decode(DataInputDeserializer.this, ctx);
        }

        @Override
        public <F> @Nullable F field(ExtraDataContext ctx, @Nullable String field, Endec<F> endec, @Nullable F defaultValue) {
            return endec.decode(DataInputDeserializer.this, ctx);
        }
    }

    private class Map<V> implements Deserializer.Map<V> {

        private final Endec<V> valueEndec;
        private final ExtraDataContext ctx;

        private final int size;

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
                    DataInputDeserializer.this.readString(ctx),
                    this.valueEndec.decode(DataInputDeserializer.this, ctx)
            );
        }
    }
}
