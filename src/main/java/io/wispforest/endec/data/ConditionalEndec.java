package io.wispforest.endec.data;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.Serializer;

public interface ConditionalEndec<T, E extends Endec<T>> {

    DataToken<?> token();

    E endec();

    boolean useForEncode(Serializer<?> serializer, ExtraDataContext ctx);

    boolean useForDecode(Deserializer<?> serializer, ExtraDataContext ctx);

    static <T1, E1 extends Endec<T1>> ConditionalEndec<T1, E1> of(DataToken<?> token, E1 endec) {
        return new ConditionalEndec<>() {
            @Override
            public DataToken<?> token() {
                return token;
            }

            @Override
            public E1 endec() {
                return endec;
            }

            @Override
            public boolean useForEncode(Serializer<?> serializer, ExtraDataContext ctx) {
                return ctx.has(token);
            }

            @Override
            public boolean useForDecode(Deserializer<?> serializer, ExtraDataContext ctx) {
                return ctx.has(token);
            }
        };
    }

    static <T1, E1 extends Endec<T1>> ConditionalEndec<T1, E1> ofBl(DataToken.Instanced<Boolean> token, E1 endec) {
        return new ConditionalEndec<>() {
            @Override
            public DataToken<?> token() {
                return token;
            }

            @Override
            public E1 endec() {
                return endec;
            }

            @Override
            public boolean useForEncode(Serializer<?> serializer, ExtraDataContext ctx) {
                return ctx.has(token) && Boolean.TRUE.equals(ctx.get(token));
            }

            @Override
            public boolean useForDecode(Deserializer<?> serializer, ExtraDataContext ctx) {
                return ctx.has(token) && Boolean.TRUE.equals(ctx.get(token));
            }
        };
    }
}
