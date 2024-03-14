package io.wispforest.endec.impl;


import io.wispforest.endec.Endec;
import io.wispforest.endec.annotations.SealedPolymorphic;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class ReflectiveEndecBuilder {

    public static final ReflectiveEndecBuilder INSTANCE = new ReflectiveEndecBuilder();

    private final Map<Class<?>, Endec<?>> CLASS_TO_ENDEC = new HashMap<>();

    public ReflectiveEndecBuilder(){
        initBase(this);
    }

    /**
     * Register {@code endec} to be used for (de)serializing instances of {@code clazz}
     */
    public <T> ReflectiveEndecBuilder register(Endec<T> endec, Class<T> clazz) {
        if (CLASS_TO_ENDEC.containsKey(clazz)) {
            throw new IllegalStateException("Class '" + clazz.getName() + "' already has an associated endec");
        }

        CLASS_TO_ENDEC.put(clazz, endec);

        return this;
    }

    /**
     * Invoke {@link #register(Endec, Class)} once for each class of {@code classes}
     */
    @SafeVarargs
    public final <T> ReflectiveEndecBuilder register(Endec<T> endec, Class<T>... classes) {
        for (var clazz : classes) register(endec, clazz);

        return this;
    }

    /**
     * Get (or potentially create) the endec associated with {@code type}. In addition
     * to {@link #get(Class)}, this method uses type parameter information to automatically
     * create endecs for maps, lists, sets and optionals.
     * <p>
     * If {@code type} is none of the above, it is simply forwarded to {@link #get(Class)}
     */
    @SuppressWarnings("unchecked")
    public Endec<?> get(Type type) {
        if (type instanceof Class<?> clazz) return get(clazz);

        var parameterized = (ParameterizedType) type;
        var raw = (Class<?>) parameterized.getRawType();
        var typeArgs = parameterized.getActualTypeArguments();

        if (raw == Map.class) {
            return typeArgs[0] == String.class
                    ? get(typeArgs[1]).mapOf()
                    : Endec.map(get(typeArgs[0]), get(typeArgs[1]));
        }

        if (raw == List.class) {
            return get(typeArgs[0]).listOf();
        }

        if (raw == Set.class) {
            //noinspection rawtypes,Convert2MethodRef
            return get(typeArgs[0]).listOf().<Set>xmap(
                    list -> (Set<?>) new HashSet<>(list),
                    set -> List.copyOf(set)
            );
        }

        if (raw == Optional.class) {
            return get(typeArgs[0]).optionalOf();
        }

        return get(raw);
    }

    /**
     * Get (or potentially create) the endec associated with {@code clazz},
     * throwing if no such endec is registered and cannot automatically be created
     * <p>
     * Classes for which endecs can be generated are: records, enums, arrays and sealed
     * classes annotated with {@link SealedPolymorphic}
     */
    public <T> Endec<T> get(Class<T> clazz) {
        var endec = getOrNull(clazz);
        if (endec == null) {
            throw new IllegalStateException("No endec available for class '" + clazz.getName() + "'");
        }

        return endec;
    }

    /**
     * Non-throwing equivalent of {@link #get(Class)}
     */
    public <T> Optional<Endec<T>> maybeGet(Class<T> clazz) {
        return Optional.ofNullable(getOrNull(clazz));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> @Nullable Endec<T> getOrNull(Class<T> clazz) {
        Endec<T> serializer = (Endec<T>) CLASS_TO_ENDEC.get(clazz);

        if (serializer == null) {
            if (Record.class.isAssignableFrom(clazz)) {
                serializer = (Endec<T>) RecordEndec.create(this, (Class<? extends Record>) clazz);
            } else if (clazz.isEnum()) {
                serializer = (Endec<T>) Endec.forEnum((Class<? extends Enum>) clazz);
            } else if (clazz.isArray()) {
                serializer = (Endec<T>) this.createArrayEndec(clazz.getComponentType());
            } else if (clazz.isAnnotationPresent(SealedPolymorphic.class)) {
                serializer = (Endec<T>) this.createSealedSerializer(clazz);
            } else {
                return null;
            }

            CLASS_TO_ENDEC.put(clazz, serializer);
        }


        return serializer;
    }

    @SuppressWarnings("unchecked")
    private Endec<?> createArrayEndec(Class<?> elementClass) {
        var elementEndec = (Endec<Object>) get(elementClass);

        return elementEndec.listOf().xmap(list -> {
            int length = list.size();
            var array = Array.newInstance(elementClass, length);
            for (int i = 0; i < length; i++) {
                Array.set(array, i, list.get(i));
            }
            return array;
        }, t -> {
            int length = Array.getLength(t);
            var list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(Array.get(t, i));
            }
            return list;
        });
    }

    private Endec<?> createSealedSerializer(Class<?> commonClass) {
        if (!commonClass.isSealed()) {
            throw new IllegalStateException("@SealedPolymorphic class must be sealed");
        }

        var permittedSubclasses = Arrays.stream(commonClass.getPermittedSubclasses()).collect(Collectors.toList());

        for (int i = 0; i < permittedSubclasses.size(); i++) {
            var clazz = permittedSubclasses.get(i);

            if (clazz.isSealed()) {
                for (var subclass : clazz.getPermittedSubclasses()) {
                    if (!permittedSubclasses.contains(subclass)) permittedSubclasses.add(subclass);
                }
            }
        }

        for (var clazz : permittedSubclasses) {
            if (!clazz.isSealed() && !Modifier.isFinal(clazz.getModifiers())) {
                throw new IllegalStateException("Subclasses of a @SealedPolymorphic class must themselves be sealed");
            }
        }

        permittedSubclasses.sort(Comparator.comparing(Class::getName));

        var serializerMap = new Int2ObjectOpenHashMap<Endec<?>>();
        var classesMap = new Reference2IntOpenHashMap<Class<?>>();

        classesMap.defaultReturnValue(-1);

        for (int i = 0; i < permittedSubclasses.size(); i++) {
            Class<?> klass = permittedSubclasses.get(i);

            serializerMap.put(i, this.get(klass));
            classesMap.put(klass, i);
        }

        return Endec.dispatched(integer -> serializerMap.get(integer.intValue()), instance -> classesMap.getInt(instance.getClass()), Endec.INT);
    }

    private static void initBase(ReflectiveEndecBuilder builder) {

        // ----------
        // Primitives
        // ----------

        builder.register(Endec.BOOLEAN, Boolean.class, boolean.class)
                .register(Endec.INT, Integer.class, int.class)
                .register(Endec.LONG, Long.class, long.class)
                .register(Endec.FLOAT, Float.class, float.class)
                .register(Endec.DOUBLE, Double.class, double.class);

        builder.register(Endec.BYTE, Byte.class, byte.class)
                .register(Endec.SHORT, Short.class, short.class)
                .register(Endec.SHORT.xmap(aShort -> (char) aShort.shortValue(), character -> (short) character.charValue()), Character.class, char.class);

        builder.register(Endec.VOID, Void.class);

        // ----
        // Misc
        // ----

        builder.register(Endec.STRING, String.class)
                .register(BuiltInEndecs.UUID, UUID.class)
                .register(BuiltInEndecs.DATE, Date.class)
                .register(BuiltInEndecs.BITSET, BitSet.class);
    }
}
