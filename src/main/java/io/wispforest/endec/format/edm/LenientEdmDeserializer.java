package io.wispforest.endec.format.edm;

import io.wispforest.endec.Endec;
import io.wispforest.endec.data.ExtraDataContext;

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
    public byte readByte(ExtraDataContext ctx) {
        return this.getValue().<Number>cast().byteValue();
    }

    @Override
    public short readShort(ExtraDataContext ctx) {
        return this.getValue().<Number>cast().shortValue();
    }

    @Override
    public int readInt(ExtraDataContext ctx) {
        return this.getValue().<Number>cast().intValue();
    }

    @Override
    public long readLong(ExtraDataContext ctx) {
        return this.getValue().<Number>cast().longValue();
    }

    // ---

    @Override
    public float readFloat(ExtraDataContext ctx) {
        return this.getValue().<Number>cast().floatValue();
    }

    @Override
    public double readDouble(ExtraDataContext ctx) {
        return this.getValue().<Number>cast().doubleValue();
    }

    // ---

    @Override
    public boolean readBoolean(ExtraDataContext ctx) {
        if(this.getValue().value() instanceof Number number){
            return number.byteValue() == 1;
        }

        return super.readBoolean(ctx);
    }


    @Override
    public <V> Optional<V> readOptional(ExtraDataContext ctx, Endec<V> endec) {
        var edmElement = this.getValue();

        if(edmElement == null){
            return Optional.empty();
        } else if(edmElement.value() instanceof Optional<?>){
            return super.readOptional(ctx, endec);
        } else {
            return Optional.of(endec.decode(this, ctx));
        }
    }
}
