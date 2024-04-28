package io.wispforest.endec.format.forwarding;

import com.google.common.collect.ImmutableMap;
import io.wispforest.endec.*;
import io.wispforest.endec.data.DataToken;
import io.wispforest.endec.data.ExtraDataContext;

import java.util.Optional;
import java.util.function.BiFunction;

public class ForwardingDeserializer<T> implements Deserializer<T> {

    private final Deserializer<T> delegate;

    protected ForwardingDeserializer(Deserializer<T> delegate) {
        this.delegate = delegate;
    }

    public Deserializer<T> delegate() {
        return this.delegate;
    }

    //--

    @Override
    public byte readByte(ExtraDataContext ctx) {
        return this.delegate.readByte(ctx);
    }

    @Override
    public short readShort(ExtraDataContext ctx) {
        return this.delegate.readShort(ctx);
    }

    @Override
    public int readInt(ExtraDataContext ctx) {
        return this.delegate.readInt(ctx);
    }

    @Override
    public long readLong(ExtraDataContext ctx) {
        return this.delegate.readLong(ctx);
    }

    @Override
    public float readFloat(ExtraDataContext ctx) {
        return this.delegate.readFloat(ctx);
    }

    @Override
    public double readDouble(ExtraDataContext ctx) {
        return this.delegate.readDouble(ctx);
    }

    @Override
    public int readVarInt(ExtraDataContext ctx) {
        return this.delegate.readVarInt(ctx);
    }

    @Override
    public long readVarLong(ExtraDataContext ctx) {
        return this.delegate.readVarLong(ctx);
    }

    @Override
    public boolean readBoolean(ExtraDataContext ctx) {
        return this.delegate.readBoolean(ctx);
    }

    @Override
    public String readString(ExtraDataContext ctx) {
        return this.delegate.readString(ctx);
    }

    @Override
    public byte[] readBytes(ExtraDataContext ctx) {
        return this.delegate.readBytes(ctx);
    }

    @Override
    public <V> Optional<V> readOptional(ExtraDataContext ctx, Endec<V> endec) {
        return this.delegate.readOptional(ctx, endec);
    }

    @Override
    public <E> Sequence<E> sequence(ExtraDataContext ctx, Endec<E> elementEndec) {
        return this.delegate.sequence(ctx, elementEndec);
    }

    @Override
    public <V> Map<V> map(ExtraDataContext ctx, Endec<V> valueEndec) {
        return this.delegate.map(ctx, valueEndec);
    }

    @Override
    public Struct struct() {
        return this.delegate.struct();
    }

    @Override
    public <V> V tryRead(BiFunction<Deserializer<T>, ExtraDataContext, V> reader, ExtraDataContext ctx) {
        return this.delegate.tryRead(reader, ctx);
    }

    private static class ForwardingSelfDescribedDeserializer<T> extends ForwardingDeserializer<T> implements SelfDescribedDeserializer<T> {
        private ForwardingSelfDescribedDeserializer(Deserializer<T> delegate) {
            super(delegate);
        }

        @Override
        public <S> void readAny(Serializer<S> visitor, ExtraDataContext ctx) {
            ((SelfDescribedDeserializer<T>) this.delegate()).readAny(visitor, ctx);
        }
    }
}
