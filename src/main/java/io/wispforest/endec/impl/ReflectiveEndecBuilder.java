package io.wispforest.endec.impl;

import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationAttributes;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.annotations.*;
import io.wispforest.endec.util.reflection.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ReflectiveEndecBuilder {

    public static final ReflectiveEndecBuilder SHARED_INSTANCE = new ReflectiveEndecBuilder();

    private final Map<Class<?>, Endec<?>> classToEndec = new LinkedHashMap<>();

    private final Map<Class<? extends Annotation>, AnnotatedContextGatherer<? extends Annotation>> classToContextGatherer = new LinkedHashMap<>();
    private final Map<Class<? extends Annotation>, AnnotatedAdjuster<? extends Annotation>> classToTypeAdjuster = new LinkedHashMap<>();
    private final Map<Class<?>, MethodTypeCheckBypass> classToAlternativeChecker = new LinkedHashMap<>();

    public ReflectiveEndecBuilder(Consumer<ReflectiveEndecBuilder> defaultsSetup) {
        defaultsSetup.accept(this);
        registerDefaults(this);
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

    public <A extends Annotation> ReflectiveEndecBuilder registerTypeAdjuster(Class<A> clazz, AnnotatedAdjuster<A> adjuster) {
        if (this.classToTypeAdjuster.containsKey(clazz)) {
            throw new IllegalStateException("Class '" + clazz.getName() + "' already has an associated AnnotatedAdjuster");
        }

        this.classToTypeAdjuster.put(clazz, adjuster);
        return this;
    }

    public <A extends Annotation> ReflectiveEndecBuilder registerContextGatherer(Class<A> clazz, AnnotatedContextGatherer<A> adjuster) {
        if (this.classToContextGatherer.containsKey(clazz)) {
            throw new IllegalStateException("Class '" + clazz.getName() + "' already has an associated AnnotatedContextGatherer");
        }

        this.classToContextGatherer.put(clazz, adjuster);
        return this;
    }

    public ReflectiveEndecBuilder registerMethodTypeCheckBypass(Class<?> clazz, String ...methodNames) {
        var validMethodsToBypass = Set.of(methodNames);

        return registerMethodTypeCheckBypass(clazz, method -> validMethodsToBypass.contains(method.getName()));
    }

    public ReflectiveEndecBuilder registerMethodTypeCheckBypass(Class<?> clazz, MethodTypeCheckBypass restorer) {
        if (this.classToAlternativeChecker.containsKey(clazz)) {
            throw new IllegalStateException("Class '" + clazz.getName() + "' already has an associated GenericRestorer");
        }

        this.classToAlternativeChecker.put(clazz, restorer);
        return this;
    }

    //--

    public SerializationContext getContext(AnnotatedElement annotatedElement) {
        return getContext(ReflectionUtils.getAnnotatedType(annotatedElement));
    }

    private SerializationContext getContext(AnnotatedType annotatedType) {
        SerializationContext context = SerializationContext.empty();

        for (var clazz : this.classToContextGatherer.keySet()) {
            context = context.and(getContextFromType(clazz, annotatedType));
        }

        return context;
    }

    private <A extends Annotation> SerializationContext getContextFromType(Class<A> annotationClazz, AnnotatedType annotatedType) {
        if(!annotatedType.isAnnotationPresent(annotationClazz)) return SerializationContext.empty();

        return ((AnnotatedContextGatherer<A>) this.classToContextGatherer.get(annotationClazz))
                .getContext(annotatedType, annotatedType.getAnnotation(annotationClazz));
    }

    //--

    MethodTypeCheckBypass getAlternativeGenericTypeCheck(Class<?> clazz) {
        var alternativeCheck = this.classToAlternativeChecker.get(clazz);

        if (alternativeCheck != null) return alternativeCheck;

        if (clazz.isAnnotationPresent(GenericTypeCheckBypass.class)) {
            var validMethodsToBypass = Set.of(clazz.getAnnotation(GenericTypeCheckBypass.class).methodsToBypass());

            return method -> validMethodsToBypass.contains(method.getName());
        }

        return MethodTypeCheckBypass.FALSE;
    }

    //--

    public Endec<?> getAnnotated(AnnotatedElement annotatedElement) {
        return getAnnotated(annotatedElement, ReflectionUtils.getBaseType(annotatedElement));
    }

    public Endec<?> getAnnotated(AnnotatedElement annotatedElement, @Nullable Type baseType) {
        var endec = getAnnotated(ReflectionUtils.getAnnotatedType(annotatedElement), baseType);

        // TODO: ATTEMPT TO KEEP BINARY COMPAT WITH OLDER ENDEC VERSIONS
        if (annotatedElement instanceof RecordComponent component && component.isAnnotationPresent(NullableComponent.class)) {
            endec = endec.nullableOf();
        }

        return endec;
    }

    public Endec<?> getAnnotated(AnnotatedType annotatedType) {
        return getAnnotated(annotatedType, null);
    }

    public Endec<?> getAnnotated(AnnotatedType annotatedType, @Nullable Type baseType) {
        var type = baseType == null ? annotatedType.getType() : baseType;

        Endec<?> endec = null;

        if(annotatedType instanceof AnnotatedArrayType annotatedArrayType) {
            Class<?> arrayClazz = (Class<?>) ((type instanceof GenericArrayType) ? baseType : type);

            if(arrayClazz == null) throw new IllegalStateException("Unable to get the required base Array class to get the component type!");

            endec = createArrayEndec(arrayClazz.componentType(), annotatedArrayType.getAnnotatedGenericComponentType());
        } else if (type instanceof Class<?> clazz) {
            endec = getOrNull(clazz);
        } else if (annotatedType instanceof AnnotatedParameterizedType annotatedParameterizedType) {
            var annotatedTypeArgs = annotatedParameterizedType.getAnnotatedActualTypeArguments();
            var parameterizedType = ((ParameterizedType) type);

            if (parameterizedType.getRawType() instanceof Class<?> clazz) {
                if (clazz.equals(Map.class)) {
                    endec = annotatedTypeArgs[0].getType() == String.class
                            ? this.getAnnotated(annotatedTypeArgs[1]).mapOf()
                            : Endec.map(this.getAnnotated(annotatedTypeArgs[0]), this.getAnnotated(annotatedTypeArgs[1]));
                } else if (clazz.equals(List.class)) {
                    endec = this.getAnnotated(annotatedTypeArgs[0]).listOf();
                } else if (clazz.equals(Set.class)) {
                    endec = this.getAnnotated(annotatedTypeArgs[0]).setOf();
                } else if (clazz.equals(Optional.class)) {
                    endec = this.getAnnotated(annotatedTypeArgs[0]).optionalOf();
                } else if(isGenericObject(clazz)) {
                    endec = ObjectEndec.create(this, clazz, type);
                } else {
                    endec = this.getOrNull(clazz);
                }
            }
        }

        if (endec == null) {
            throw new IllegalStateException("No Endec available for the given type '" + type + "'");
        }

        endec = adjustEndecWithType(annotatedType, endec);

        return endec;
    }

    private <T> Endec<T> adjustEndecWithType(AnnotatedType annotatedType, Endec<T> endec) {
        for (var clazz : this.classToTypeAdjuster.keySet()) {
            var results = applyAdjusterIfPresent(clazz, annotatedType, endec);

            if (!results.allowFutherAdjustments()) return results.endec();

            if (!results.equals(AdjustmentResult.empty())) endec = results.endec();
        }

        return endec;
    }

    private <A extends Annotation, T> AdjustmentResult<T> applyAdjusterIfPresent(Class<A> annotationClazz, AnnotatedType annotatedType, Endec<T> endec) {
        if(!annotatedType.isAnnotationPresent(annotationClazz)) return AdjustmentResult.empty();

        return ((AnnotatedAdjuster<A>) this.classToTypeAdjuster.get(annotationClazz))
                .adjustEndec(annotatedType, annotatedType.getAnnotation(annotationClazz), endec);
    }

    //--

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
        if (type instanceof AnnotatedElement annotatedElement) return this.getAnnotated(annotatedElement);

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

        if (Record.class.isAssignableFrom(raw)) {
            return RecordEndec.create(this, (Class<? extends Record>) raw, typeArgs);
        }

        if (isGenericObject(raw)) {
            return ObjectEndec.create(this, raw, typeArgs);
        }

        return this.get(raw);
    }

    private static boolean isGenericObject(Class<?> clazz) {
        return !(Record.class.isAssignableFrom(clazz)
                || clazz.isEnum()
                || clazz.isArray()
                || clazz.isAnnotationPresent(SealedPolymorphic.class));
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
        Endec<T> endec = (Endec<T>) this.classToEndec.get(clazz);

        if (endec != null) return endec;

        endec = (Endec<T>) getDefinedEndec(clazz);

        if (endec == null) {
            if (Record.class.isAssignableFrom(clazz)) {
                endec = (Endec<T>) RecordEndec.create(this, (Class<? extends Record>) clazz);
            } else if (clazz.isEnum()) {
                endec = (Endec<T>) Endec.forEnum((Class<? extends Enum>) clazz);
            } else if (clazz.isArray()) {
                endec = (Endec<T>) this.createArrayEndec(clazz.getComponentType(), null);
            } else if (clazz.isAnnotationPresent(SealedPolymorphic.class)) {
                endec = (Endec<T>) this.createSealedEndec(clazz);
            } else {
                endec = ObjectEndec.create(this, clazz);
            }
        }

        this.classToEndec.put(clazz, endec);

        return endec;
    }

    <T> Endec<T> getExistingEndec(Class<T> clazz) {
        return (Endec<T>) this.classToEndec.get(clazz);
    }

    private Endec<?> getDefinedEndec(Class<?> clazz) {
        var possibleEndecGetterFields = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(DefinedEndecGetter.class))
                .toList();

        var possibleEndecGetterMethods = Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.isAnnotationPresent(DefinedEndecGetter.class))
                .toList();

        if (possibleEndecGetterFields.size() > 1) throw new IllegalStateException("Multiple DefinedEndecGetter fields within the given class: " + clazz);
        if (possibleEndecGetterMethods.size() > 1) throw new IllegalStateException("Multiple DefinedEndecGetter methods within the given class: " + clazz);

        if (!possibleEndecGetterFields.isEmpty()) {
            var field = possibleEndecGetterFields.get(0);
            var modifiers = field.getModifiers();

            if (!Modifier.isStatic(modifiers) && !Modifier.isPublic(modifiers)) {
                throw new IllegalStateException("A DefinedEndecGetter field was found not be PUBLIC and/or STATIC which is required!");
            }

            var isEndec = ReflectionUtils.isTypeCompatible(ReflectionUtils.createParameterizedType(Endec.class, clazz), field.getGenericType());
            var isStructEndec = ReflectionUtils.isTypeCompatible(ReflectionUtils.createParameterizedType(StructEndec.class, clazz), field.getGenericType());

            if (!isEndec && !isStructEndec) {
                throw new IllegalStateException("Unable to use the given field [" + field + "] due to the type not matching the following class: " + clazz);
            }

            try {
                return (Endec<?>) field.get(null);
            } catch (Throwable e) {
                throw new RuntimeException("A clazz [" + clazz + "] with a Defined Endec field was unable to be gotten due to an error!", e);
            }
        } else if (!possibleEndecGetterMethods.isEmpty()) {
            var method = possibleEndecGetterMethods.get(0);
            var modifiers = method.getModifiers();

            if (!Modifier.isStatic(modifiers) && !Modifier.isPublic(modifiers)) {
                throw new IllegalStateException("A DefinedEndecGetter method was found not be PUBLIC and/or STATIC which is required!");
            }

            var isEndec = ReflectionUtils.isTypeCompatible(ReflectionUtils.createParameterizedType(Endec.class, clazz), method.getGenericReturnType());
            var isStructEndec = ReflectionUtils.isTypeCompatible(ReflectionUtils.createParameterizedType(StructEndec.class, clazz), method.getGenericReturnType());

            if (!isEndec && !isStructEndec) {
                throw new IllegalStateException("Unable to use the given field [" + method + "] due to the type not matching the following class: " + clazz);
            }

            try {
                return (Endec<?>) method.invoke(null);
            } catch (Throwable e) {
                throw new RuntimeException("A clazz [" + clazz + "] with a Defined Endec field was unable to be gotten due to an error!", e);
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Endec<?> createArrayEndec(Class<?> elementClass, @Nullable AnnotatedType genericComponentType) {
        if(elementClass.equals(byte.class) || elementClass.equals(Byte.class)) return Endec.BYTES;

        var elementEndec = (Endec<Object>) ((genericComponentType == null) ? this.get(elementClass) : this.getAnnotated(genericComponentType));

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
        var namedMap = new Object2IntOpenHashMap<String>();

        classesMap.defaultReturnValue(-1);

        for (int i = 0; i < permittedSubclasses.size(); i++) {
            Class<?> klass = permittedSubclasses.get(i);

            serializerMap.put(i, this.get(klass));

            classesMap.put(klass, i);

            if(namedMap.containsKey(klass.getSimpleName())) {
                var existingIndex = namedMap.getInt(klass.getSimpleName());

                Class<?> existingKlass = null;

                for (var classEntry : classesMap.reference2IntEntrySet()) {
                    if (classEntry.getIntValue() == existingIndex) {
                        existingKlass = classEntry.getKey();

                        break;
                    }
                }

                throw new IllegalStateException("Unable to handled the given set of sealed class as two or more class share the same name! [Class1: " + existingKlass.getName() + ", Class2: " + klass.getName() + "]");
            }

            namedMap.put(klass.getSimpleName(), i);
        }

        return Endec.<Object>ifAttr(
                        SerializationAttributes.HUMAN_READABLE,
                        Endec.dispatched(name -> serializerMap.get(namedMap.getInt(name)), instance -> instance.getClass().getName(), Endec.STRING)
                ).orElse(
                        Endec.dispatched(integer -> serializerMap.get(integer.intValue()), instance -> classesMap.getInt(instance.getClass()), Endec.INT)
                );
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

        builder.registerTypeAdjuster(IsNullable.class, new AnnotatedAdjuster<>() {
            @Override
            public <T> AdjustmentResult<T> adjustEndec(AnnotatedType annotatedType, IsNullable annotation, Endec<T> base) {
                return AdjustmentResult.of(new OptionalEndec<>(base.optionalOf(), () -> null, annotation.mayOmitField()));
            }
        });

        builder.registerTypeAdjuster(IsVarInt.class, new AnnotatedAdjuster<>() {
            @Override
            public <T> AdjustmentResult<T> adjustEndec(AnnotatedType annotatedType, IsVarInt annotation, Endec<T> base) {
                if (annotatedType.getType() instanceof Class<?> clazz) {
                    if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
                        return AdjustmentResult.of((Endec<T>) Endec.VAR_INT);
                    } else if (clazz.equals(long.class) || clazz.equals(Long.class)) {
                        return AdjustmentResult.of((Endec<T>) Endec.VAR_LONG);
                    }
                }

                return AdjustmentResult.empty();
            }
        });

        builder.registerTypeAdjuster(RangedFloat.class, new AnnotatedAdjuster<>() {
            @Override
            public <T> AdjustmentResult<T> adjustEndec(AnnotatedType annotatedType, RangedFloat annotation, Endec<T> base) {
                if (annotatedType.getType() instanceof Class<?> clazz) {
                    if(ReflectionUtils.isIntegerType(clazz)) {
                        throw new IllegalStateException("Can not apply RangedFloat to a integer type, use RangedInteger instead!");
                    } else if(ReflectionUtils.isFloatType(clazz)) {
                        return AdjustmentResult.of((Endec<T>) rangedUnsafe(base, ReflectionUtils.castFloat(clazz, annotation.min()), ReflectionUtils.castFloat(clazz, annotation.max()), annotation.throwError()));
                    }
                }

                return AdjustmentResult.empty();
            }
        });

        builder.registerTypeAdjuster(RangedInteger.class, new AnnotatedAdjuster<>() {
            @Override
            public <T> AdjustmentResult<T> adjustEndec(AnnotatedType annotatedType, RangedInteger annotation, Endec<T> base) {
                if (annotatedType.getType() instanceof Class<?> clazz) {
                    if(ReflectionUtils.isFloatType(clazz)) {
                        throw new IllegalStateException("Can not apply RangedInteger to a integer type, use RangedFloat instead!");
                    } else if(ReflectionUtils.isIntegerType(clazz)) {
                        return AdjustmentResult.of((Endec<T>) rangedUnsafe(base, ReflectionUtils.castInteger(clazz, annotation.min()), ReflectionUtils.castInteger(clazz, annotation.max()), annotation.throwError()));
                    }
                }

                return AdjustmentResult.empty();
            }
        });

        builder.registerContextGatherer(Comment.class, (annotatedType, annotation) -> {
            return SerializationContext.attributes(new CommentAttribute(annotation.comment()));
        });
    }

    private static <N extends Number & Comparable<N>> Endec<N> rangedUnsafe(Endec<?> endec, @Nullable Number min, @Nullable Number max, boolean throwError) {
        return Endec.ranged((Endec<N>) endec, (N) min, (N) max, throwError);
    }
}
