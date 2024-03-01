package io.wispforest.endec.format.forwarding;

import io.wispforest.endec.DataTokenHolder;
import io.wispforest.endec.Endec;
import io.wispforest.endec.ExtraDataSerializer;
import io.wispforest.endec.Serializer;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class ForwardingSerializer<T> extends ExtraDataSerializer<T> {

    private final Serializer<T> delegate;

    protected ForwardingSerializer(Serializer<T> delegate) {
        this.delegate = delegate;
    }

    public Serializer<T> delegate() {
        return this.delegate;
    }

    public static <T> ForwardingSerializer<T> of(Serializer<T> delegate, Stream<DataTokenHolder<?>> tokens) {
        var forwardingSerializer = new ForwardingSerializer<>(delegate);

        tokens.forEach(holder -> holder.consume(forwardingSerializer::set));

        return forwardingSerializer;
    }


    //--

    @Override
    public void writeByte(byte value) {
        this.delegate.writeByte(value);
    }

    @Override
    public void writeShort(short value) {
        this.delegate.writeShort(value);
    }

    @Override
    public void writeInt(int value) {
        this.delegate.writeInt(value);
    }

    @Override
    public void writeLong(long value) {
        this.delegate.writeLong(value);
    }

    @Override
    public void writeFloat(float value) {
        this.delegate.writeFloat(value);
    }

    @Override
    public void writeDouble(double value) {
        this.delegate.writeDouble(value);
    }

    @Override
    public void writeVarInt(int value) {
        this.delegate.writeVarInt(value);
    }

    @Override
    public void writeVarLong(long value) {
        this.delegate.writeVarLong(value);
    }

    @Override
    public void writeBoolean(boolean value) {
        this.delegate.writeBoolean(value);
    }

    @Override
    public void writeString(String value) {
        this.delegate.writeString(value);
    }

    @Override
    public void writeBytes(byte[] bytes) {
        this.delegate.writeBytes(bytes);
    }

    @Override
    public <V> void writeOptional(Endec<V> endec, Optional<V> optional) {
        this.delegate.writeOptional(endec, optional);
    }

    @Override
    public <E> Sequence<E> sequence(Endec<E> elementEndec, int size) {
        return this.delegate.sequence(elementEndec, size);
    }

    @Override
    public <V> Map<V> map(Endec<V> valueEndec, int size) {
        return this.delegate.map(valueEndec, size);
    }

    @Override
    public Struct struct() {
        return this.delegate.struct();
    }

    @Override
    public T result() {
        return this.delegate.result();
    }
}
