package io.wispforest.endec.impl;

import io.wispforest.endec.*;
import io.wispforest.endec.util.reflection.ObjectConstructor;
import io.wispforest.endec.util.reflection.ReflectionUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;

public final class ObjectEndec<T> implements StructEndec<T> {

    private final ObjectConstructor<T> constructor;
    private final List<StructField.MutableField<T, ?>> fields;

    private ObjectEndec(Constructor<T> constructor, List<StructField.MutableField<T, ?>> fields) {
        this.constructor = ObjectConstructor.fromConstructor(constructor);
        this.fields = fields;
    }

    public static <T> StructEndec<T> createShared(Class<T> clazz, Type ...typeArguments){
        return create(ReflectiveEndecBuilder.SHARED_INSTANCE, clazz, typeArguments);
    }

    public static <T> StructEndec<T> create(ReflectiveEndecBuilder builder, Class<T> clazz) {
        return create(builder, clazz, new Type[0]);
    }

    /**
     * Create (or get, if it already exists) the endec for the given object type
     */
    @SuppressWarnings("unchecked")
    public static <T> StructEndec<T> create(ReflectiveEndecBuilder builder, Class<T> clazz, Type ...typeArguments) {
        var extraTypeInfoStack = new ArrayDeque<>(List.of(typeArguments));

        var fields = new LinkedHashMap<Field, @Nullable Type>();
        ObjectType objectType = null;

        for (var unpackClassParent : ReflectionUtils.unpackClassStack(clazz)) {
            for (var field : unpackClassParent.getDeclaredFields()) {
                var modifiers = field.getModifiers();

                if (Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) continue;

                fields.put(field, (field.getGenericType() instanceof TypeVariable<?>) ? extraTypeInfoStack.poll() : null);

                if (objectType == null) {
                    objectType = Modifier.isFinal(modifiers) ? ObjectType.RECORD : ObjectType.STRUCT;
                } else if(objectType == ObjectType.RECORD && !Modifier.isFinal(modifiers)) {
                    throw new IllegalStateException("Unable to create endec due to mismatching field privileges as such can't be recordified with a invalid field: " + field);
                }
            }
        }

        if (objectType == null) {
            throw new IllegalStateException("Unable to create endec for the given endec due to inablity to figure out the objects type: " + clazz);
        }

        if (objectType == ObjectType.STRUCT) {
            Map<Field, StructField.MutableField<T, ?>> validFields = new LinkedHashMap<>();

            for (var entry : fields.entrySet()) {
                Field field = entry.getKey();

                var modifiers = field.getModifiers();

                if (Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) continue;

                var alternativeGenericTypeCheck = builder.getAlternativeGenericTypeCheck(clazz);

                validFields.put(field, new StructField.MutableField<>(
                        field.getName(),
                        (Endec<Object>) builder.getAnnotated(field, entry.getValue()),
                        ReflectionUtils.createGetter(clazz, field, alternativeGenericTypeCheck),
                        ReflectionUtils.createSetter(clazz, field, alternativeGenericTypeCheck)
                ));
            }

            Constructor<T> noArgConstructor = null;
            Constructor<T> validConstructor = null;

            for (var constructor : clazz.getConstructors()) {
                if (validFields.size() == constructor.getParameterCount() && validConstructor == null) {
                    var constructorTypes = List.of(constructor.getParameterTypes());
                    var fieldTypes = validFields.keySet().stream().map(Field::getType).toList();

                    if (constructorTypes.equals(fieldTypes) /*&& constructor.getAnnotation(SerializationConstructor.class) != null*/) {
                        validConstructor = (Constructor<T>) constructor;
                    }
                } else if (constructor.getParameterCount() == 0) {
                    noArgConstructor = (Constructor<T>) constructor;
                }
            }

            if (validConstructor != null) {
                return new RecordishEndec<>(validConstructor, (List<StructField<T, ?>>) (Object) validFields.values().stream().toList());
            } else if (noArgConstructor != null) {
                return new ObjectEndec<>(noArgConstructor, validFields.values().stream().toList());
            }
        } else {
            var structFields = new ArrayList<StructField<T, ?>>();

            for (var entry : fields.entrySet()) {
                Field field = entry.getKey();

                structFields.add(new StructField<>(
                        field.getName(),
                        (Endec<Object>) builder.getAnnotated(field, entry.getValue()),
                        ReflectionUtils.createGetter(clazz, field, builder.getAlternativeGenericTypeCheck(clazz))
                ));
            }

            var validConstructor = (Constructor<T>) Arrays.stream(clazz.getConstructors())
                    .filter(constructor -> {
                        if (fields.size() == constructor.getParameterCount()) {
                            var constructorTypes = List.of(constructor.getParameterTypes());
                            var fieldTypes = fields.keySet().stream().map(Field::getType).toList();

                            return constructorTypes.equals(fieldTypes) /*&& constructor.getAnnotation(SerializationConstructor.class) != null*/;
                        }

                        return false;
                    })
                    .findFirst()
                    .orElse(null);

            if (validConstructor != null) {
                return new RecordishEndec<>(validConstructor, structFields);
            }
        }

        throw new IllegalStateException("Unable to contruct a endec for the given class object due to the inability to find the needed Constructor: " + clazz);
    }

    @Override
    public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, T value) {
        for (StructField.MutableField<T, ?> field : fields) {
            field.encodeField(ctx, serializer, struct, value);
        }
    }

    @Override
    public T decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
        T t = constructor.createInstance();

        for (StructField.MutableField<T, ?> field : fields) {
            field.decodeField(ctx, deserializer, struct, t);
        }

        return t;
    }

    private enum ObjectType {
        STRUCT,
        RECORD
    }
}
