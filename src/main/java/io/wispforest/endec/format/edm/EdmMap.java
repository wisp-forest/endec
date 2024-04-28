package io.wispforest.endec.format.edm;


import io.wispforest.endec.data.ExtraDataContext;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.endec.util.MapCarrier;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

public final class EdmMap extends EdmElement<Map<String, EdmElement<?>>> implements MapCarrier {

    private final Map<String, EdmElement<?>> map;

    EdmMap(Map<String, EdmElement<?>> map) {
        super(Collections.unmodifiableMap(map), Type.MAP);

        this.map = map;
    }

    @Override
    public <T> T getWithErrors(@NotNull KeyedEndec<T> key, ExtraDataContext ctx) {
        if (!this.has(key)) return key.defaultValue();
        return key.endec().<EdmElement<?>>decodeFully(EdmDeserializer::of, this.map.get(key.key()), ctx.instances());
    }

    @Override
    public <T> void put(@NotNull KeyedEndec<T> key, ExtraDataContext ctx, @NotNull T value) {
        this.map.put(key.key(), key.endec().encodeFully(EdmSerializer::of, value, ctx.instances()));
    }

    @Override
    public <T> void delete(@NotNull KeyedEndec<T> key) {
        this.map.remove(key.key());
    }

    @Override
    public <T> boolean has(@NotNull KeyedEndec<T> key) {
        return this.map.containsKey(key.key());
    }
}
