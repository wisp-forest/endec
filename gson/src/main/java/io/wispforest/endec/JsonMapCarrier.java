package io.wispforest.endec;

import com.google.gson.JsonObject;
import io.wispforest.endec.format.json.JsonDeserializer;
import io.wispforest.endec.format.json.JsonSerializer;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.endec.util.MapCarrier;
import org.jetbrains.annotations.NotNull;

public class JsonMapCarrier implements MapCarrier {

    private final JsonObject object;

    public JsonMapCarrier(JsonObject object) {
        this.object = object;
    }

    @Override
    public <T> T getWithErrors(@NotNull KeyedEndec<T> key) {
        return this.object.has(key.key()) ? key.endec().decodeFully(JsonDeserializer::of, this.object.get(key.key())) : key.defaultValue();
    }

    @Override
    public <T> void put(@NotNull KeyedEndec<T> key, @NotNull T value) {
        this.object.add(key.key(), key.endec().encodeFully(JsonSerializer::of, value));
    }

    @Override
    public <T> void delete(@NotNull KeyedEndec<T> key) {
        this.object.remove(key.key());
    }

    @Override
    public <T> boolean has(@NotNull KeyedEndec<T> key) {
        return this.object.has(key.key());
    }
}
