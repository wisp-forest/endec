package io.wispforest.endec.format.forwarding;

import com.google.common.collect.ImmutableMap;
import io.wispforest.endec.data.DataToken;
import io.wispforest.endec.Endec;
import io.wispforest.endec.ExtraDataSerializer;
import io.wispforest.endec.Serializer;

import java.util.Optional;

public class ForwardingSerializer<T> extends ExtraDataSerializer<T> {

    private final Serializer<T> delegate;

    protected ForwardingSerializer(Serializer<T> delegate, DataToken.Instance ...instances) {
        super(instances);

        this.delegate = delegate;
    }

    public static <T> Serializer<T> of(Serializer<T> delegate, DataToken.Instance ...instances) {
        if(instances.length == 0) return delegate;

        return new ForwardingSerializer<>(delegate, instances);
    }

    @Override
    public java.util.Map<DataToken<?>, Object> allTokens() {
        var builder = ImmutableMap.<DataToken<?>, Object>builder();

        builder.putAll(this.delegate.allTokens());
        builder.putAll(super.allTokens());

        return builder.build();
    }

    public Serializer<T> delegate() {
        return this.delegate;
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
