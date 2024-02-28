package io.wispforest.endec.format.edm;


import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.endec.util.MapCarrier;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class EdmMap extends EdmElement<Map<String, EdmElement<?>>> implements MapCarrier {

    EdmMap(EdmElement<Map<String, EdmElement<?>>> mapElement) {
        super(mapElement.value(), mapElement.type());
    }

    @Override
    public <T> T getWithErrors(@NotNull KeyedEndec<T> key) {
        if (!this.has(key)) return key.defaultValue();
        return key.endec().decodeFully(EdmDeserializer::of, this.value().get(key.key()));
    }

    @Override
    public <T> void put(@NotNull KeyedEndec<T> key, @NotNull T value) {
        this.value().put(key.key(), key.endec().encodeFully(EdmSerializer::of, value));
    }

    @Override
    public <T> void delete(@NotNull KeyedEndec<T> key) {
        this.value().remove(key.key());
    }

    @Override
    public <T> boolean has(@NotNull KeyedEndec<T> key) {
        return this.value().containsKey(key.key());
    }
}
