package io.wispforest.endec.impl;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.util.reflection.ObjectConstructor;

import java.lang.reflect.Constructor;
import java.util.List;

/// 
/// A base for both [RecordEndec]s and any object found to be closer to a Record with [ObjectEndec#create] method 
/// 
public class RecordishEndec<T> implements StructEndec<T> {

    protected final List<StructField<T, ?>> fields;
    protected final ObjectConstructor<T> instanceCreator;

    protected RecordishEndec(Constructor<T> constructor, List<StructField<T, ?>> fields) {
        this.instanceCreator = ObjectConstructor.fromConstructor(constructor);
        this.fields = fields;
    }

    @Override
    public T decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
        Object[] fieldValues = new Object[this.fields.size()];

        for (int i = 0; i < this.fields.size(); i++) {
            fieldValues[i] = this.fields.get(i).decodeField(ctx, deserializer, struct);
        }

        return instanceCreator.createInstance(fieldValues);
    }

    @Override
    public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, T instance) {
        this.fields.forEach(field -> field.encodeField(ctx, serializer, struct, instance));
    }
}
