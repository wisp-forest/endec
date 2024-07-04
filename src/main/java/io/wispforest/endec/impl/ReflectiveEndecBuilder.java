package io.wispforest.endec.impl;

import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationAttributes;
import io.wispforest.endec.annotations.InnerLookup;
import io.wispforest.endec.annotations.NullableComponent;
import io.wispforest.endec.annotations.SealedPolymorphic;
import io.wispforest.endec.annotations.VariableInteger;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ReflectiveEndecBuilder {

    public static final ReflectiveEndecBuilder SHARED_INSTANCE = new ReflectiveEndecBuilder();

    private final Map<Class<?>, Endec<?>> classToEndec = new HashMap<>();
    private final Map<Class<? extends Annotation>, ReflectiveEndecAdjuster<? extends Annotation>> classToAdjuster = new HashMap<>();

    public ReflectiveEndecBuilder(Consumer<ReflectiveEndecBuilder> defaultsSetup) {
        defaultsSetup.accept(this);
        registerDefaults(this);
        registerDefaultAdjusters(this);
    }

    public ReflectiveEndecBuilder() {
        this(reflectiveEndecBuilder -> {});
    }

    /**
     * Register {@code endec} to be used for (de)serializing instances of {@code clazz}
     */
    public <T> ReflectiveEndecBuilder register(Endec<T> endec, Class<T> clazz) {
        if (this.classToEndec.containsKey(clazz)) {
            throw new IllegalStateException("Class '" + clazz.getName() + "' already has an associated endec");
        }

        this.classToEndec.put(clazz, endec);
        return this;
    }

    /**
     * Invoke {@link #register(Endec, Class)} once for each class of {@code classes}
     */
    @SafeVarargs
    public final <T> ReflectiveEndecBuilder register(Endec<T> endec, Class<T>... classes) {
        for (var clazz : classes) this.register(endec, clazz);
        return this;
    }

    /**
     * Register {@code endec} to be used for (de)serializing instances of {@code clazz}
     */
    public <A extends Annotation> ReflectiveEndecBuilder registerAdjuster(ReflectiveEndecAdjuster<A> adjuster, Class<A> clazz) {
        if (this.classToAdjuster.containsKey(clazz)) {
            throw new IllegalStateException("Class '" + clazz.getName() + "' already has an associated adjuster");
        }

        this.classToAdjuster.put(clazz, adjuster);
        return this;
    }

    /**
     * Invoke {@link #register(Endec, Class)} once for each class of {@code classes}
     */
    @SafeVarargs
    public final <A extends Annotation> ReflectiveEndecBuilder registerAdjuster(ReflectiveEndecAdjuster<A> adjuster, Class<A>... classes) {
        for (var clazz : classes) this.registerAdjuster(adjuster, clazz);
        return this;
    }

    public Endec<?> get(AnnotatedElement element, Type type) {
        Endec<?> endec = null;

        try {
            endec = get(type);
        } catch (IllegalStateException ignore) {}

        endec = adjustEndec(element, endec);

        if (endec == null) {
            Class<?> clazz = (Class<?>) ((type instanceof Class<?>) ? (type) : ((ParameterizedType) type).getRawType());

            throw new IllegalStateException("No endec available for class '" + clazz.getName() + "'");
        }

        return endec;
    }

    @Nullable
    private <T> Endec<T> adjustEndec(AnnotatedElement element, @Nullable Endec<T> endec) {
        for (var clazz : this.classToAdjuster.keySet()) {
            var adjustedEndec = getAndRead(clazz, element, endec);

            if(adjustedEndec != null) return adjustedEndec;
        }

        return endec;
    }

    @Nullable
    private <A extends Annotation, T> Endec<T> getAndRead(Class<A> annotationClazz, AnnotatedElement element, @Nullable Endec<T> endec) {
        if(!element.isAnnotationPresent(annotationClazz)) return null;

        return ((ReflectiveEndecAdjuster<A>) this.classToAdjuster.get(annotationClazz))
                .adjustEndec(element, element.getAnnotation(annotationClazz), endec);
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
        if (type instanceof Class<?> clazz) return this.get(clazz);

        var parameterized = (ParameterizedType) type;
        var raw = (Class<?>) parameterized.getRawType();
        var typeArgs = parameterized.getActualTypeArguments();

        if (raw == Map.class) {
            return typeArgs[0] == String.class
                    ? this.get(typeArgs[1]).mapOf()
                    : Endec.map(this.get(typeArgs[0]), this.get(typeArgs[1]));
        }

        if (raw == List.class) {
            return this.get(typeArgs[0]).listOf();
        }

        if (raw == Set.class) {
            //noinspection rawtypes,Convert2MethodRef
            return this.get(typeArgs[0]).listOf().<Set>xmap(
                    list -> (Set<?>) new HashSet<>(list),
                    set -> List.copyOf(set)
            );
        }

        if (raw == Optional.class) {
            return this.get(typeArgs[0]).optionalOf();
        }

        return this.get(raw);
    }

    /**
     * Get (or potentially create) the endec associated with {@code clazz},
     * throwing if no such endec is registered and cannot automatically be created
     * <p>
     * Classes for which endecs can be generated are: records, enums, arrays and sealed
     * classes annotated with {@link SealedPolymorphic}
     */
    public <T> Endec<T> get(Class<T> clazz) {
        var endec = this.getOrNull(clazz);
        if (endec == null) {
            throw new IllegalStateException("No endec available for class '" + clazz.getName() + "'");
        }

        return endec;
    }

    /**
     * Non-throwing equivalent of {@link #get(Class)}
     */
    public <T> Optional<Endec<T>> maybeGet(Class<T> clazz) {
        return Optional.ofNullable(this.getOrNull(clazz));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> @Nullable Endec<T> getOrNull(Class<T> clazz) {
        Endec<T> serializer = (Endec<T>) this.classToEndec.get(clazz);

        if (serializer == null) {
            if (Record.class.isAssignableFrom(clazz)) {
                serializer = (Endec<T>) RecordEndec.create(this, (Class<? extends Record>) clazz);
            } else if (clazz.isEnum()) {
                serializer = (Endec<T>) Endec.forEnum((Class<? extends Enum>) clazz);
            } else if (clazz.isArray()) {
                serializer = (Endec<T>) this.createArrayEndec(clazz.getComponentType());
            } else if (clazz.isAnnotationPresent(SealedPolymorphic.class)) {
                serializer = (Endec<T>) this.createSealedEndec(clazz);
            } else {
                return null;
            }

            this.classToEndec.put(clazz, serializer);
        }


        return serializer;
    }

    @SuppressWarnings("unchecked")
    private Endec<?> createArrayEndec(Class<?> elementClass) {
        var elementEndec = (Endec<Object>) this.get(elementClass);

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

    private Endec<?> createSealedEndec(Class<?> commonClass) {
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

    @SafeVarargs
    private <T> ReflectiveEndecBuilder registerIfMissing(Endec<T> endec, Class<T>... classes) {
        for (var clazz : classes) {
            this.classToEndec.putIfAbsent(clazz, endec);
        }

        return this;
    }

    private static void registerDefaults(ReflectiveEndecBuilder builder) {

        // ----------
        // Primitives
        // ----------

        builder.registerIfMissing(Endec.BOOLEAN, Boolean.class, boolean.class)
                .registerIfMissing(Endec.INT, Integer.class, int.class)
                .registerIfMissing(Endec.LONG, Long.class, long.class)
                .registerIfMissing(Endec.FLOAT, Float.class, float.class)
                .registerIfMissing(Endec.DOUBLE, Double.class, double.class);

        builder.registerIfMissing(Endec.BYTE, Byte.class, byte.class)
                .registerIfMissing(Endec.SHORT, Short.class, short.class)
                .registerIfMissing(Endec.SHORT.xmap(aShort -> (char) aShort.shortValue(), character -> (short) character.charValue()), Character.class, char.class);

        builder.registerIfMissing(Endec.VOID, Void.class);

        // ----
        // Misc
        // ----

        builder.registerIfMissing(Endec.STRING, String.class)
                .registerIfMissing(BuiltInEndecs.UUID, UUID.class)
                .registerIfMissing(BuiltInEndecs.DATE, Date.class)
                .registerIfMissing(BuiltInEndecs.BITSET, BitSet.class);
    }

    private static void registerDefaultAdjusters(ReflectiveEndecBuilder builder) {
        builder.registerAdjuster(
                new ReflectiveEndecAdjuster<>() {
                    @Override
                    @Nullable
                    public <T> Endec<T> adjustEndec(AnnotatedElement element, NullableComponent annotationInst, Endec<T> base) {
                        Class<?> type;

                        if(element instanceof Field field) {
                            type = field.getType();
                        } else if(element instanceof RecordComponent recordComponent) {
                            type = recordComponent.getType();
                        } else {
                            return null;
                        }

                        if(type.isPrimitive()) throw new IllegalStateException("Unable to create nullable Endec variant of a primitive type! [Element: " + element.toString() + "]");

                        return base.nullableOf();
                    }
                }, NullableComponent.class);

        builder.registerAdjuster(
                new ReflectiveEndecAdjuster<>() {
                    @Override
                    @Nullable
                    public <T> Endec<T> adjustEndec(AnnotatedElement element, VariableInteger annotationInst, Endec<T> base) {
                        if(base == Endec.INT) {
                            if(annotationInst.ignoreHumanReadable()) return (Endec<T>) Endec.VAR_INT;

                            return Endec.ifAttr(SerializationAttributes.HUMAN_READABLE, base)
                                    .orElse((Endec<T>) Endec.VAR_INT);
                        } else if (base == Endec.LONG) {
                            if(annotationInst.ignoreHumanReadable()) return (Endec<T>) Endec.VAR_LONG;

                            return Endec.ifAttr(SerializationAttributes.HUMAN_READABLE, base)
                                    .orElse((Endec<T>) Endec.VAR_LONG);
                        }

                        return null;
                    }
                }, VariableInteger.class);

    }
}
