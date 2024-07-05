package io.wispforest.endec.impl;

import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationAttributes;
import io.wispforest.endec.annotations.InnerLookup;
import io.wispforest.endec.annotations.IsNullable;
import io.wispforest.endec.annotations.SealedPolymorphic;
import io.wispforest.endec.annotations.IsVarInt;
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

    private final Map<Class<? extends Annotation>, TypedAdjuster<? extends Annotation>> classToTypeAdjuster = new HashMap<>();

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

    public <A extends Annotation> ReflectiveEndecBuilder registerTypeAdjuster(TypedAdjuster<A> adjuster, Class<A> clazz) {
        if (this.classToTypeAdjuster.containsKey(clazz)) {
            throw new IllegalStateException("Class '" + clazz.getName() + "' already has an associated adjuster");
        }

        this.classToTypeAdjuster.put(clazz, adjuster);
        return this;
    }

    @SafeVarargs
    public final <A extends Annotation> ReflectiveEndecBuilder registerTypeAdjuster(TypedAdjuster<A> adjuster, Class<A>... classes) {
        for (var clazz : classes) this.registerTypeAdjuster(adjuster, clazz);
        return this;
    }

    @Nullable
    private <T> Endec<T> adjustEndecWithType(AnnotatedType annotatedType, @Nullable Endec<T> endec) {
        for (var clazz : this.classToTypeAdjuster.keySet()) {
            var adjustedEndec = applyTypeAdjusterIfPresent(clazz, annotatedType, endec);

            if(adjustedEndec != null) return adjustedEndec;
        }

        return endec;
    }

    @Nullable
    private <A extends Annotation, T> Endec<T> applyTypeAdjusterIfPresent(Class<A> annotationClazz, AnnotatedType annotatedType, @Nullable Endec<T> endec) {
        if(!annotatedType.isAnnotationPresent(annotationClazz)) return null;

        return ((TypedAdjuster<A>) this.classToTypeAdjuster.get(annotationClazz))
                .adjustEndec(annotatedType, annotatedType.getAnnotation(annotationClazz), endec);
    }

    //--

    private AnnotatedType getAnnotatedType(AnnotatedElement annotatedElement) {
        return switch (annotatedElement) {
            case Parameter parameter -> parameter.getAnnotatedType();
            case Field field -> field.getAnnotatedType();
            case RecordComponent recordComponent -> recordComponent.getAnnotatedType();
            default -> throw new IllegalStateException("Unable to find the annotated type for the given Annotated Element: [Element: " + annotatedElement + "]");
        };
    }

    private Class<?> getBaseType(AnnotatedElement annotatedElement) {
        return switch (annotatedElement) {
            case Parameter parameter -> parameter.getType();
            case Field field -> field.getType();
            case RecordComponent recordComponent -> recordComponent.getType();
            default -> throw new IllegalStateException("Unable to find the annotated type for the given Annotated Element: [Element: " + annotatedElement + "]");
        };
    }

    public Endec<?> getAnnotated(AnnotatedElement annotatedElement) {
        return getAnnotated(getAnnotatedType(annotatedElement), getBaseType(annotatedElement), annotatedElement);
    }

    public Endec<?> getAnnotated(AnnotatedType annotatedType) {
        return getAnnotated(annotatedType, null,null);
    }

    private Endec<?> getAnnotated(AnnotatedType annotatedType, @Nullable Class<?> baseType, @Nullable AnnotatedElement annotatedElement) {
        var type = annotatedType.getType();

        Endec<?> endec;

        if(annotatedType instanceof AnnotatedArrayType annotatedArrayType) {
            Class<?> arrayClazz = (type instanceof GenericArrayType) ? baseType : (Class<?>) type;

            if(arrayClazz == null) throw new IllegalStateException("Unable to get the required base Array class to get the component type!");

            endec = createArrayEndec(arrayClazz.componentType(), annotatedArrayType.getAnnotatedGenericComponentType());
        } else if (type instanceof Class<?> clazz) {
            endec = getOrNull(clazz);
        } else {
            var annotatedTypeArgs = ((AnnotatedParameterizedType) annotatedType).getAnnotatedActualTypeArguments();

            var annotatedType0 = annotatedTypeArgs[0];

            var raw = ((ParameterizedType) type).getRawType();

            endec = switch (raw) {
                case Class<?> clazz when clazz.equals(Map.class) -> {
                    var annotatedType1 = annotatedTypeArgs[1];

                    yield annotatedType0.getType() == String.class
                            ? this.getAnnotated(annotatedType1).mapOf()
                            : Endec.map(this.getAnnotated(annotatedType0), this.getAnnotated(annotatedType1));
                }
                case Class<?> clazz when clazz.equals(List.class) -> this.getAnnotated(annotatedType0).listOf();
                case Class<?> clazz when clazz.equals(Set.class) -> this.getAnnotated(annotatedType0).setOf();
                case Class<?> clazz when clazz.equals(Optional.class) -> this.getAnnotated(annotatedType0).optionalOf();
                case Class<?> clazz -> this.getOrNull(clazz);
                default -> throw new IllegalStateException("Unexpected value: " + raw);
            };
        }

        endec = adjustEndecWithType(annotatedType, endec);

        if (endec == null) {
            throw new IllegalStateException("No Endec available for the given type '" + type.toString() + "'");
        }

        return endec;
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
                serializer = (Endec<T>) this.createArrayEndec(clazz.getComponentType(), null);
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
    private Endec<?> createArrayEndec(Class<?> componentType, @Nullable AnnotatedType genericComponentType) {
        if(componentType.equals(byte.class) || componentType.equals(Byte.class)) return Endec.BYTES;

        var elementEndec = (Endec<Object>) ((genericComponentType == null) ? this.get(componentType) : this.getAnnotated(genericComponentType));

        return elementEndec.listOf().xmap(list -> {
            int length = list.size();
            var array = Array.newInstance(componentType, length);
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

            if (!clazz.isSealed()) continue;

            for (var subclass : clazz.getPermittedSubclasses()) {
                if (!permittedSubclasses.contains(subclass)) permittedSubclasses.add(subclass);
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
        for (var clazz : classes) this.classToEndec.putIfAbsent(clazz, endec);

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
        builder.registerTypeAdjuster(
                new TypedAdjuster<>() {
                    @Override
                    @Nullable
                    public <T> Endec<T> adjustEndec(AnnotatedType annotatedType, IsNullable annotationInst, Endec<T> base) {
                        Type type = annotatedType.getType();

                        if(type instanceof Class<?> clazz && clazz.isPrimitive()) throw new IllegalStateException("Unable to create nullable Endec variant of a primitive type! [Element: " + annotatedType.toString() + "]");

                        return base.nullableOf();
                    }
                }, IsNullable.class);

        builder.registerTypeAdjuster(
                new TypedAdjuster<>() {
                    @Override
                    @Nullable
                    public <T> Endec<T> adjustEndec(AnnotatedType annotatedType, IsVarInt annotationInst, Endec<T> base) {
                        if(base == Endec.INT) {
                            if(annotationInst.ignoreHumanReadable()) return (Endec<T>) Endec.VAR_INT;

                            return Endec.ifAttr(SerializationAttributes.HUMAN_READABLE, base)
                                    .orElse((Endec<T>) Endec.VAR_INT);
                        } else if (base == Endec.LONG) {
                            if(annotationInst.ignoreHumanReadable()) return (Endec<T>) Endec.VAR_LONG;

                            return Endec.ifAttr(SerializationAttributes.HUMAN_READABLE, base)
                                    .orElse((Endec<T>) Endec.VAR_LONG);
                        }

                        throw new IllegalStateException("Unable to handle the given type passed!");
                    }
                }, IsVarInt.class);


    }
}
