package io.wispforest.endec.format.forwarding;

import io.wispforest.endec.Endec;
import io.wispforest.endec.SelfDescribedSerializer;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.Serializer;

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
    public void writeByte(SerializationContext ctx, byte value) {
        this.delegate.writeByte(ctx, value);
    }

    @Override
    public void writeShort(SerializationContext ctx, short value) {
        this.delegate.writeShort(ctx, value);
    }

    @Override
    public void writeInt(SerializationContext ctx, int value) {
        this.delegate.writeInt(ctx, value);
    }

    @Override
    public void writeLong(SerializationContext ctx, long value) {
        this.delegate.writeLong(ctx, value);
    }

    @Override
    public void writeFloat(SerializationContext ctx, float value) {
        this.delegate.writeFloat(ctx, value);
    }

    @Override
    public void writeDouble(SerializationContext ctx, double value) {
        this.delegate.writeDouble(ctx, value);
    }

    @Override
    public void writeVarInt(SerializationContext ctx, int value) {
        this.delegate.writeVarInt(ctx, value);
    }

    @Override
    public void writeVarLong(SerializationContext ctx, long value) {
        this.delegate.writeVarLong(ctx, value);
    }

    @Override
    public void writeBoolean(SerializationContext ctx, boolean value) {
        this.delegate.writeBoolean(ctx, value);
    }

    @Override
    public void writeString(SerializationContext ctx, String value) {
        this.delegate.writeString(ctx, value);
    }

    @Override
    public void writeBytes(SerializationContext ctx, byte[] bytes) {
        this.delegate.writeBytes(ctx, bytes);
    }

    @Override
    public <V> void writeOptional(SerializationContext ctx, Endec<V> endec, Optional<V> optional) {
        this.delegate.writeOptional(ctx, endec, optional);
    }

    @Override
    public <E> Sequence<E> sequence(SerializationContext ctx, Endec<E> elementEndec, int size) {
        return this.delegate.sequence(ctx, elementEndec, size);
    }

    @Override
    public <V> Map<V> map(SerializationContext ctx, Endec<V> valueEndec, int size) {
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
