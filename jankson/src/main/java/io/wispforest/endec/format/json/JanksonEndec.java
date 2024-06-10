package io.wispforest.endec.format.json;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.api.SyntaxError;
import io.wispforest.endec.*;
import io.wispforest.endec.SerializationContext;

public final class JanksonEndec implements Endec<JsonElement> {

    private static final Jankson JANKSON = new Jankson.Builder().build();
    public static final JanksonEndec INSTANCE = new JanksonEndec();

    private JanksonEndec() {}

    @Override
    public void encode(SerializationContext ctx, Serializer<?> serializer, JsonElement value) {
        if (serializer instanceof SelfDescribedSerializer<?>) {
            JanksonDeserializer.of(value).readAny(ctx, serializer);
            return;
        }

        serializer.writeString(ctx, value.toString());
    }

    @Override
    public JsonElement decode(SerializationContext ctx, Deserializer<?> deserializer) {
        if (deserializer instanceof SelfDescribedDeserializer<?> selfDescribedDeserializer) {
            var json = JanksonSerializer.of();
            selfDescribedDeserializer.readAny(ctx, json);

            return json.result();
        }

        try {
            return JANKSON.load(deserializer.readString(ctx));
        } catch (SyntaxError error) {
            throw new RuntimeException(error);
        }
    }
}
