package io.wispforest.endec.format.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;
import io.wispforest.endec.*;
import io.wispforest.endec.data.DataTokens;
import io.wispforest.endec.data.ExtraDataContext;

public final class JsonEndec implements Endec<JsonElement> {

    public static final JsonEndec INSTANCE = new JsonEndec();

    private JsonEndec() {}

    @Override
    public void encode(Serializer<?> serializer, ExtraDataContext ctx, JsonElement value) {
        if (serializer instanceof SelfDescribedSerializer<?>) {
            JsonDeserializer.of(value).readAny(serializer, ctx);
            return;
        }

        serializer.writeString(ctx, value.toString());
    }

    @Override
    public JsonElement decode(Deserializer<?> deserializer, ExtraDataContext ctx) {
        if (deserializer instanceof SelfDescribedDeserializer<?> selfDescribedDeserializer) {
            var json = JsonSerializer.of();
            selfDescribedDeserializer.readAny(json, ctx);

            return json.result();
        }

        return new JsonStreamParser(deserializer.readString(ctx)).next();
    }
}
