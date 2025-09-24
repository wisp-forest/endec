package io.wispforest.endec.format.edm;

import com.google.common.collect.Lists;
import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;

import java.util.*;

import static io.wispforest.endec.format.edm.EdmElement.Type.*;

public class LenientEdmDeserializer extends EdmDeserializer {

    protected LenientEdmDeserializer(EdmElement<?> serialized) {
        super(serialized);
    }

    public static LenientEdmDeserializer of(EdmElement<?> serialized) {
        return new LenientEdmDeserializer(serialized);
    }

    private static final EdmElement.Type[] NUMBER_TYPES = new EdmElement.Type[]{I8, U8, I16, U16, I32, U32, I64, U64, F32, F64};

    // ---

    @Override
    public byte readByte(SerializationContext ctx) {
        return this.getNumber(ctx).byteValue();
    }

    @Override
    public short readShort(SerializationContext ctx) {
        return this.getNumber(ctx).shortValue();
    }

    @Override
    public int readInt(SerializationContext ctx) {
        return this.getNumber(ctx).intValue();
    }

    @Override
    public long readLong(SerializationContext ctx) {
        return this.getNumber(ctx).longValue();
    }

    // ---

    @Override
    public float readFloat(SerializationContext ctx) {
        return this.getNumber(ctx).floatValue();
    }

    @Override
    public double readDouble(SerializationContext ctx) {
        return this.getNumber(ctx).doubleValue();
    }

    // ---

    public Number getNumber(SerializationContext ctx) {
        return this.getValueForType(ctx, Arrays.asList(NUMBER_TYPES));
    }

    // ---

    @Override
    public boolean readBoolean(SerializationContext ctx) {
        var value = this.getValueForType(ctx, Lists.asList(BOOLEAN, NUMBER_TYPES));

        if(value instanceof Number number){
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
        var value = this.getValueForType(ctx, List.of(BYTES, SEQUENCE));

        List<EdmElement<?>> list;

        if(value instanceof byte[] array) {
            list = new ArrayList<>();

            for (byte b : array) list.add(EdmElement.i8(b));
        } else if(value instanceof List){
            list = this.getValue().cast();
        } else {
            throw new IllegalStateException("Unable to handle the given value for sequence within LenientEdmDeserializer!");
        }

        return new Sequence<>(ctx, elementEndec, list);
    }
}
