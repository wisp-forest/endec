package io.wispforest.endec.format.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;
import io.wispforest.endec.*;

public final class JsonEndec implements Endec<JsonElement> {

    public static final JsonEndec INSTANCE = new JsonEndec();

    private JsonEndec() {}

    @Override
    public void encode(Serializer<?> serializer, JsonElement value) {
        if (serializer.attributes().contains(SerializationAttribute.SELF_DESCRIBING)) {
            JsonDeserializer.of(value).readAny(serializer);
            return;
        }

        serializer.writeString(value.toString());
    }

    @Override
    public JsonElement decode(Deserializer<?> deserializer) {
        if (deserializer instanceof SelfDescribedDeserializer<?> selfDescribedDeserializer) {
            var json = JsonSerializer.of();
            selfDescribedDeserializer.readAny(json);

            return json.result();
        }

        return new JsonStreamParser(deserializer.readString()).next();
    }
}
