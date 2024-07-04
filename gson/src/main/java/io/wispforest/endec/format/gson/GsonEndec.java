package io.wispforest.endec.format.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;
import io.wispforest.endec.*;
import io.wispforest.endec.SerializationContext;

public final class GsonEndec implements Endec<JsonElement> {

    public static final GsonEndec INSTANCE = new GsonEndec();

    private GsonEndec() {}

    @Override
    public void encode(SerializationContext ctx, Serializer<?> serializer, JsonElement value) {
        if (serializer instanceof SelfDescribedSerializer<?>) {
            GsonDeserializer.of(value).readAny(ctx, serializer);
            return;
        }

        serializer.writeString(ctx, value.toString());
    }

    @Override
    public JsonElement decode(SerializationContext ctx, Deserializer<?> deserializer) {
        if (deserializer instanceof SelfDescribedDeserializer<?> selfDescribedDeserializer) {
            var json = GsonSerializer.of();
            selfDescribedDeserializer.readAny(ctx, json);

            return json.result();
        }

        return new JsonStreamParser(deserializer.readString(ctx)).next();
    }
}
