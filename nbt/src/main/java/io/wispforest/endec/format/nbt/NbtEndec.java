package io.wispforest.endec.format.nbt;

import com.google.common.io.ByteStreams;
import io.wispforest.endec.Deserializer;
import io.wispforest.endec.SelfDescribedDeserializer;
import io.wispforest.endec.SerializationAttribute;
import io.wispforest.endec.Serializer;

import java.io.IOException;

public final class NbtEndec implements Endec<NbtElement> {

    public static final Endec<NbtElement> ELEMENT = new NbtEndec();
    public static final Endec<NbtCompound> COMPOUND = new NbtEndec().xmap(NbtCompound.class::cast, compound -> compound);

    private NbtEndec() {}

    @Override
    public void encode(Serializer<?> serializer, NbtElement value) {
        if (serializer.attributes().contains(SerializationAttribute.SELF_DESCRIBING)) {
            NbtDeserializer.of(value).readAny(serializer);
            return;
        }

        try {
            var output = ByteStreams.newDataOutput();
            NbtIo.writeForPacket(value, output);

            serializer.writeBytes(output.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode binary NBT in NbtEndec", e);
        }
    }

    @Override
    public NbtElement decode(Deserializer<?> deserializer) {
        if (deserializer instanceof SelfDescribedDeserializer<?> selfDescribedDeserializer) {
            var nbt = NbtSerializer.of();
            selfDescribedDeserializer.readAny(nbt);

            return nbt.result();
        }

        try {
            return NbtIo.read(ByteStreams.newDataInput(deserializer.readBytes()), NbtTagSizeTracker.ofUnlimitedBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse binary NBT in NbtEndec", e);
        }
    }
}
