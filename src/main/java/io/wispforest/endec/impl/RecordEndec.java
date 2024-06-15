package io.wispforest.endec.impl;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.annotations.NullableComponent;
import io.wispforest.endec.SerializationContext;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RecordEndec<R extends Record> implements StructEndec<R> {

    private static final Map<Class<?>, RecordEndec<?>> ENDECS = new HashMap<>();

    private final List<StructField<R, ?>> fields;
    private final Constructor<R> instanceCreator;

    private RecordEndec(Constructor<R> instanceCreator, List<StructField<R, ?>> fields) {
        this.instanceCreator = instanceCreator;
        this.fields = fields;
    }

    public static <R extends Record> RecordEndec<R> createShared(Class<R> recordClass){
        return create(ReflectiveEndecBuilder.SHARED_INSTANCE, recordClass);
    }

    /**
     * Create (or get, if it already exists) the endec for the given record type
     */
    @SuppressWarnings("unchecked")
    public static <R extends Record> RecordEndec<R> create(ReflectiveEndecBuilder builder, Class<R> recordClass) {
        if (ENDECS.containsKey(recordClass)) return (RecordEndec<R>) ENDECS.get(recordClass);

        var fields = new ArrayList<StructField<R, ?>>();
        var canonicalConstructorArgs = new Class<?>[recordClass.getRecordComponents().length];

        var lookup = MethodHandles.publicLookup();
        for (int i = 0; i < recordClass.getRecordComponents().length; i++) {
            try {
                var component = recordClass.getRecordComponents()[i];
                var handle = lookup.unreflect(component.getAccessor());

                var endec = (Endec<Object>) builder.get(component.getGenericType());
                if(component.isAnnotationPresent(NullableComponent.class)) endec = endec.nullableOf();

                fields.add(new StructField<>(component.getName(), endec, instance -> getRecordEntry(instance, handle)));

                canonicalConstructorArgs[i] = component.getType();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to create method handle for record component accessor", e);
            }
        }

        try {
            var endec = new RecordEndec<>(recordClass.getConstructor(canonicalConstructorArgs), fields);
            ENDECS.put(recordClass, endec);

            return endec;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Could not locate canonical record constructor");
        }
    }

    private static <R extends Record> Object getRecordEntry(R instance, MethodHandle accessor) {
        try {
            return accessor.invoke(instance);
        } catch (Throwable e) {
            throw new IllegalStateException("Unable to get record component value", e);
        }
    }

    @Override
    public R decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
        Object[] fieldValues = new Object[this.fields.size()];

        int index = 0;

        for (var field : this.fields) {
            fieldValues[index++] = field.decodeField(ctx, deserializer, struct);
        }

        try {
            return instanceCreator.newInstance(fieldValues);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Error while deserializing record", e);
        }
    }

    @Override
    public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, R instance) {
        this.fields.forEach(field -> field.encodeField(ctx, serializer, struct, instance));
    }
}
