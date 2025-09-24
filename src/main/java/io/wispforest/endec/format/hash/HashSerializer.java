package io.wispforest.endec.format.hash;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.Serializer;

import java.util.Optional;

public class HashSerializer implements Serializer<HashCode> {

    private final HashFunction function;
    private final Hasher hasher;

    protected HashSerializer(HashFunction function) {
        this.function = function;
        this.hasher = function.newHasher();
    }

    public static HashSerializer of(HashFunction function) {
        return new HashSerializer(function);
    }

    public static HashSerializer crc32c() {
        return new HashSerializer(Hashing.crc32c());
    }

    public static <T> long toCrc32cHash(Endec<T> endec, T t) {
        return endec.encodeFully(HashSerializer::crc32c, t).asLong();
    }

    @Override
    public void writeByte(SerializationContext ctx, byte value) {
        hasher.putByte(value);
    }

    @Override
    public void writeShort(SerializationContext ctx, short value) {
        hasher.putShort(value);
    }

    @Override
    public void writeInt(SerializationContext ctx, int value) {
        hasher.putInt(value);
    }

    @Override
    public void writeLong(SerializationContext ctx, long value) {
        hasher.putLong(value);
    }

    @Override
    public void writeFloat(SerializationContext ctx, float value) {
        hasher.putFloat(value);
    }

    @Override
    public void writeDouble(SerializationContext ctx, double value) {
        hasher.putDouble(value);
    }

    @Override
    public void writeVarInt(SerializationContext ctx, int value) {
        writeInt(ctx, value);
    }

    @Override
    public void writeVarLong(SerializationContext ctx, long value) {
        writeLong(ctx, value);
    }

    @Override
    public void writeBoolean(SerializationContext ctx, boolean value) {
        hasher.putBoolean(value);
    }

    @Override
    public void writeString(SerializationContext ctx, String value) {
        hasher.putUnencodedChars(value);
    }

    @Override
    public void writeBytes(SerializationContext ctx, byte[] bytes) {
        hasher.putBytes(bytes);
    }

    @Override
    public <V> void writeOptional(SerializationContext ctx, Endec<V> endec, Optional<V> optional) {
        hasher.putBoolean(optional.isPresent());

        optional.ifPresent(v -> endec.encode(ctx, this, v));
    }

    @Override
    public <E> Sequence<E> sequence(SerializationContext ctx, Endec<E> elementEndec, int size) {
        hasher.putInt(size);

        return new Sequence<E>() {
            @Override
            public void element(E element) {
                elementEndec.encode(ctx, HashSerializer.this, element);
            }

            @Override public void end() {}
        };
    }

    @Override
    public <V> Map<V> map(SerializationContext ctx, Endec<V> valueEndec, int size) {
        hasher.putInt(size);

        return new Map<V>() {
            @Override
            public void entry(String key, V value) {
                hasher.putUnencodedChars(key);

                valueEndec.encode(ctx, HashSerializer.this, value);
            }

            @Override public void end() {}
        };
    }

    @Override
    public Struct struct() {
        return new Struct() {
            @Override
            public <F> Struct field(String name, SerializationContext ctx, Endec<F> endec, F value, boolean mayOmit) {
                hasher.putUnencodedChars(name);

                endec.encode(ctx, HashSerializer.this, value);

                return this;
            }

            @Override public void end() {}
        };
    }

    @Override
    public HashCode result() {
        return hasher.hash();
    }
}