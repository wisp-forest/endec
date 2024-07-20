package io.wispforest.endec.format.edm;

import com.google.common.io.ByteStreams;
import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class LenientEdmDeserializer extends EdmDeserializer {

    protected LenientEdmDeserializer(EdmElement<?> serialized) {
        super(serialized);
    }

    public static LenientEdmDeserializer of(EdmElement<?> serialized) {
        return new LenientEdmDeserializer(serialized);
    }

    // ---

    @Override
    public byte readByte(SerializationContext ctx) {
        return this.getValue().<Number>cast().byteValue();
    }

    @Override
    public short readShort(SerializationContext ctx) {
        return this.getValue().<Number>cast().shortValue();
    }

    @Override
    public int readInt(SerializationContext ctx) {
        return this.getValue().<Number>cast().intValue();
    }

    @Override
    public long readLong(SerializationContext ctx) {
        return this.getValue().<Number>cast().longValue();
    }

    // ---

    @Override
    public float readFloat(SerializationContext ctx) {
        return this.getValue().<Number>cast().floatValue();
    }

    @Override
    public double readDouble(SerializationContext ctx) {
        return this.getValue().<Number>cast().doubleValue();
    }

    // ---

    @Override
    public boolean readBoolean(SerializationContext ctx) {
        if(this.getValue().value() instanceof Number number){
            return number.byteValue() == 1;
        }

        return super.readBoolean(ctx);
    }

    @Override
    public <V> Optional<V> readOptional(SerializationContext ctx, Endec<V> endec) {
        var edmElement = this.getValue();

        if(edmElement == null){
            return Optional.empty();
        } else if(edmElement.value() instanceof Optional<?>){
            return super.readOptional(ctx, endec);
        } else {
            return Optional.of(endec.decode(ctx, this));
        }
    }

    //--

    @Override
    public <E> Deserializer.Sequence<E> sequence(SerializationContext ctx, Endec<E> elementEndec) {
        var value = this.getValue().value();

        List<EdmElement<?>> list;

        if(value instanceof byte[] array) {
            list = new ArrayList<>();

            for (byte b : array) list.add(EdmElement.wrapByte(b));
        } else if(value instanceof List){
            list = this.getValue().cast();
        } else {
            throw new IllegalStateException("Unable to handle the given value for sequence within LenientEdmDeserializer!");
        }

        return new Sequence<>(ctx, elementEndec, list);
    }
}
