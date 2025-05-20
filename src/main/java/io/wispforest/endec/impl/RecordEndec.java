package io.wispforest.endec.impl;

import io.wispforest.endec.Endec;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

public final class RecordEndec<R extends Record> extends RecordishEndec<R> {

    private static final Map<Class<?>, RecordEndec<?>> ENDECS = new HashMap<>();

    private RecordEndec(Constructor<R> instanceCreator, List<StructField<R, ?>> fields) {
        super(instanceCreator, fields);
    }

    public static <R extends Record> RecordEndec<R> createShared(Class<R> recordClass){
        return create(ReflectiveEndecBuilder.SHARED_INSTANCE, recordClass);
    }

    public static <R extends Record> RecordEndec<R> create(ReflectiveEndecBuilder builder, Class<R> recordClass) {
        return create(builder, recordClass, new Type[0]);
    }

    /**
     * Create (or get, if it already exists) the endec for the given record type
     */
    @SuppressWarnings("unchecked")
    public static <R extends Record> RecordEndec<R> create(ReflectiveEndecBuilder builder, Class<R> recordClass, Type ...typeArguments) {
        var extraTypeInfoStack = new ArrayDeque<Type>(List.of(typeArguments));

        var endec = builder.getExistingEndec(recordClass);

        if (endec instanceof RecordEndec<R> recordEndec) return recordEndec;

        var fields = new ArrayList<StructField<R, ?>>();
        var canonicalConstructorArgs = new Class<?>[recordClass.getRecordComponents().length];

        var lookup = MethodHandles.publicLookup();
        for (int i = 0; i < recordClass.getRecordComponents().length; i++) {
            try {
                var component = recordClass.getRecordComponents()[i];
                var handle = lookup.unreflect(component.getAccessor());

                var type = (component.getGenericType() instanceof TypeVariable<?>) ? extraTypeInfoStack.poll() : null;

                fields.add(
                        new StructField<>(
                                component.getName(),
                                (Endec<Object>) builder.getAnnotated(component, type),
                                instance -> getRecordEntry(instance, handle),
                                null,
                                builder.getContext(component)
                        )
                );

                canonicalConstructorArgs[i] = component.getType();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to create method handle for record component accessor", e);
            }
        }

        try {
            return new RecordEndec<>(recordClass.getConstructor(canonicalConstructorArgs), fields);
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
}
