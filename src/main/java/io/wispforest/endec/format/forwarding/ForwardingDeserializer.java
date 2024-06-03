package io.wispforest.endec.format.forwarding;

import io.wispforest.endec.*;
import io.wispforest.endec.SerializationContext;

import java.util.Optional;

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
    public byte readByte(SerializationContext ctx) {
        return this.delegate.readByte(ctx);
    }

    @Override
    public short readShort(SerializationContext ctx) {
        return this.delegate.readShort(ctx);
    }

    @Override
    public int readInt(SerializationContext ctx) {
        return this.delegate.readInt(ctx);
    }

    @Override
    public long readLong(SerializationContext ctx) {
        return this.delegate.readLong(ctx);
    }

    @Override
    public float readFloat(SerializationContext ctx) {
        return this.delegate.readFloat(ctx);
    }

    @Override
    public double readDouble(SerializationContext ctx) {
        return this.delegate.readDouble(ctx);
    }

    @Override
    public int readVarInt(SerializationContext ctx) {
        return this.delegate.readVarInt(ctx);
    }

    @Override
    public long readVarLong(SerializationContext ctx) {
        return this.delegate.readVarLong(ctx);
    }

    @Override
    public boolean readBoolean(SerializationContext ctx) {
        return this.delegate.readBoolean(ctx);
    }

    @Override
    public String readString(SerializationContext ctx) {
        return this.delegate.readString(ctx);
    }

    @Override
    public byte[] readBytes(SerializationContext ctx) {
        return this.delegate.readBytes(ctx);
    }

    @Override
    public <V> Optional<V> readOptional(SerializationContext ctx, Endec<V> endec) {
        return this.delegate.readOptional(ctx, endec);
    }

    @Override
    public <E> Sequence<E> sequence(SerializationContext ctx, Endec<E> elementEndec) {
        return this.delegate.sequence(ctx, elementEndec);
    }

    @Override
    public <V> Map<V> map(SerializationContext ctx, Endec<V> valueEndec) {
        return this.delegate.map(ctx, valueEndec);
    }

    @Override
    public Struct struct() {
        return this.delegate.struct();
    }

    @Override
    public <V> V tryRead(SerializationContext ctx, Decoder<V> decoder) {
        return this.delegate.tryRead(ctx, decoder);
    }

    private static class ForwardingSelfDescribedDeserializer<T> extends ForwardingDeserializer<T> implements SelfDescribedDeserializer<T> {
        private ForwardingSelfDescribedDeserializer(Deserializer<T> delegate) {
            super(delegate);
        }

        @Override
        public <S> void readAny(SerializationContext ctx, Serializer<S> visitor) {
            ((SelfDescribedDeserializer<T>) this.delegate()).readAny(ctx, visitor);
        }
    }
}
