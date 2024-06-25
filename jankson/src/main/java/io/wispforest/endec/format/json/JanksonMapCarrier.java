package io.wispforest.endec.format.json;

import blue.endless.jankson.JsonObject;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.endec.util.MapCarrier;
import org.jetbrains.annotations.NotNull;

public class JanksonMapCarrier implements MapCarrier {

    private final JsonObject object;

    public JanksonMapCarrier(JsonObject object) {
        this.object = object;
    }

    @Override
    public <T> T getWithErrors(SerializationContext ctx, @NotNull KeyedEndec<T> key) {
        return this.object.containsKey(key.key()) ? key.endec().decodeFully(ctx, JanksonDeserializer::of, this.object.get(key.key())) : key.defaultValue();
    }

    @Override
    public <T> void put(SerializationContext ctx, @NotNull KeyedEndec<T> key, @NotNull T value) {
        this.object.put(key.key(), key.endec().encodeFully(ctx, JanksonSerializer::of, value));
    }

    @Override
    public <T> void delete(@NotNull KeyedEndec<T> key) {
        this.object.remove(key.key());
    }

    @Override
    public <T> boolean has(@NotNull KeyedEndec<T> key) {
        return this.object.containsKey(key.key());
    }
}
