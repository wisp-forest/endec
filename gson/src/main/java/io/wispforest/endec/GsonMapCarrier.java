package io.wispforest.endec;

import com.google.gson.JsonObject;
import io.wispforest.endec.format.json.GsonDeserializer;
import io.wispforest.endec.format.json.GsonSerializer;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.endec.util.MapCarrier;
import org.jetbrains.annotations.NotNull;

public class GsonMapCarrier implements MapCarrier {

    private final JsonObject object;

    public GsonMapCarrier(JsonObject object) {
        this.object = object;
    }

    @Override
    public <T> T getWithErrors(SerializationContext ctx, @NotNull KeyedEndec<T> key) {
        return this.object.has(key.key()) ? key.endec().decodeFully(ctx, GsonDeserializer::of, this.object.get(key.key())) : key.defaultValue();
    }

    @Override
    public <T> void put(SerializationContext ctx, @NotNull KeyedEndec<T> key, @NotNull T value) {
        this.object.add(key.key(), key.endec().encodeFully(ctx, GsonSerializer::of, value));
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
