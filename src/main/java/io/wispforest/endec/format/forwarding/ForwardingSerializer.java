package io.wispforest.endec.format.forwarding;

import io.wispforest.endec.*;
import io.wispforest.endec.data.ExtraDataContext;

import java.util.Optional;

public class ForwardingSerializer<T> implements Serializer<T> {

    private final Serializer<T> delegate;

    protected ForwardingSerializer(Serializer<T> delegate) {
        this.delegate = delegate;
    }

    public Serializer<T> delegate() {
        return this.delegate;
    }

    //--

    @Override
    public void writeByte(ExtraDataContext ctx, byte value) {
        this.delegate.writeByte(ctx, value);
    }

    @Override
    public void writeShort(ExtraDataContext ctx, short value) {
        this.delegate.writeShort(ctx, value);
    }

    @Override
    public void writeInt(ExtraDataContext ctx, int value) {
        this.delegate.writeInt(ctx, value);
    }

    @Override
    public void writeLong(ExtraDataContext ctx, long value) {
        this.delegate.writeLong(ctx, value);
    }

    @Override
    public void writeFloat(ExtraDataContext ctx, float value) {
        this.delegate.writeFloat(ctx, value);
    }

    @Override
    public void writeDouble(ExtraDataContext ctx, double value) {
        this.delegate.writeDouble(ctx, value);
    }

    @Override
    public void writeVarInt(ExtraDataContext ctx, int value) {
        this.delegate.writeVarInt(ctx, value);
    }

    @Override
    public void writeVarLong(ExtraDataContext ctx, long value) {
        this.delegate.writeVarLong(ctx, value);
    }

    @Override
    public void writeBoolean(ExtraDataContext ctx, boolean value) {
        this.delegate.writeBoolean(ctx, value);
    }

    @Override
    public void writeString(ExtraDataContext ctx, String value) {
        this.delegate.writeString(ctx, value);
    }

    @Override
    public void writeBytes(ExtraDataContext ctx, byte[] bytes) {
        this.delegate.writeBytes(ctx, bytes);
    }

    @Override
    public <V> void writeOptional(ExtraDataContext ctx, Endec<V> endec, Optional<V> optional) {
        this.delegate.writeOptional(ctx, endec, optional);
    }

    @Override
    public <E> Sequence<E> sequence(ExtraDataContext ctx, Endec<E> elementEndec, int size) {
        return this.delegate.sequence(ctx, elementEndec, size);
    }

    @Override
    public <V> Map<V> map(ExtraDataContext ctx, Endec<V> valueEndec, int size) {
        return this.delegate.map(ctx, valueEndec, size);
    }

    @Override
    public Struct struct() {
        return this.delegate.struct();
    }

    @Override
    public T result() {
        return this.delegate.result();
    }

    private static class ForwardingSelfDescribedSerializer<T> extends ForwardingSerializer<T> implements SelfDescribedSerializer<T> {
        private ForwardingSelfDescribedSerializer(Serializer<T> delegate) {
            super(delegate);
        }
    }
}
