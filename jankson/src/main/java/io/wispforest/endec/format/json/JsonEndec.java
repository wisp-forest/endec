package io.wispforest.endec.format.json;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.api.SyntaxError;
import io.wispforest.endec.*;

public final class JsonEndec implements Endec<JsonElement> {

    private final Jankson DEFAULT = new Jankson.Builder().build();

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

        try {
            return DEFAULT.load(deserializer.readString());
        } catch (SyntaxError error) {
            throw new RuntimeException(error);
        }
    }
}
