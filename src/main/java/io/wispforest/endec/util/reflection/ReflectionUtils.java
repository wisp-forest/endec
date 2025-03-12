package io.wispforest.endec.util.reflection;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class ReflectionUtils {

    public static Type getBaseType(AnnotatedElement annotatedElement) {
        if (annotatedElement instanceof Parameter parameter) {
            return parameter.getType();
        } else if (annotatedElement instanceof Field field) {
            return field.getGenericType();
        } else if (annotatedElement instanceof RecordComponent component) {
            return component.getGenericType();
        }

        throw new IllegalStateException("Unable to get the required type from the follow AnnotatedElement: " + annotatedElement.toString());
    }

    public static AnnotatedType getAnnotatedType(AnnotatedElement annotatedElement) {
        if (annotatedElement instanceof Parameter parameter) {
            return parameter.getAnnotatedType();
        } else if (annotatedElement instanceof Field field) {
            return field.getAnnotatedType();
        } else if (annotatedElement instanceof RecordComponent component) {
            return component.getAnnotatedType();
        }

        throw new IllegalStateException("Unable to get the required AnnotatedType from the follow AnnotatedElement: " + annotatedElement.toString());
    }

    public static List<Class<?>> unpackClassStack(Class<?> clazz) {
        Deque<Class<?>> classes = new ArrayDeque<>();

        var superclass = clazz.getSuperclass();

        while (superclass != null) {
            classes.push(superclass);

            superclass = superclass.getSuperclass();
        }

        classes.offerLast(clazz);

        return List.copyOf(classes);
    }

    public static List<Class<?>> unpackClassesAndInterfaces(Class<?> clazz) {
        List<Class<?>> classes = new ArrayList<>();

        var superclass = clazz.getSuperclass();

        while (superclass != null) {
            classes.add(superclass);

            classes.addAll(List.of(superclass.getInterfaces()));

            superclass = superclass.getSuperclass();
        }

        classes.add(clazz);

        return List.copyOf(classes);
    }

    public static ParameterizedType createParameterizedType(Type rawType, Type ...typeArguments) {
        return new ParameterizedType() {
            @Override public Type[] getActualTypeArguments() { return typeArguments; }
            @Override public Type getRawType() { return rawType; }
            @Override public Type getOwnerType() { return null; }
        };
    }

    public static boolean isIntegerType(Class<?> clazz) {
        return clazz.equals(byte.class) || clazz.equals(Byte.class)
                || clazz.equals(short.class) || clazz.equals(Short.class)
                || clazz.equals(int.class) || clazz.equals(Integer.class)
                || clazz.equals(long.class) || clazz.equals(Long.class);
    }

    public static boolean isFloatType(Class<?> clazz) {
        return clazz.equals(float.class) || clazz.equals(Float.class)
                || clazz.equals(double.class) || clazz.equals(Double.class);
    }

    public static boolean isNumber(Class<?> clazz) {
        return isIntegerType(clazz) || isFloatType(clazz);
    }

    public static Number castFloat(Class<?> clazz, double number) {
        if (clazz.equals(float.class) || clazz.equals(Float.class)) {
            return ((Number) number).floatValue();
        }

        return number;
    }

    public static Number castInteger(Class<?> clazz, long number) {
        if (clazz.equals(byte.class) || clazz.equals(Byte.class)) {
            return ((Number) number).byteValue();
        } else if (clazz.equals(short.class) || clazz.equals(Short.class)) {
            return ((Number) number).shortValue();
        } else if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
            return ((Number) number).intValue();
        }

        return number;
    }

    public static boolean areTypesEqual(Type type1, Type type2) {
        if (type1.equals(type2)) return true;

        if (type1.equals(boolean.class) || type1.equals(Boolean.class)) return type2.equals(boolean.class) || type2.equals(Boolean.class);
        if (type1.equals(char.class) || type1.equals(Character.class)) return type2.equals(char.class) || type2.equals(Character.class);
        if (type1.equals(byte.class) || type1.equals(Byte.class)) return type2.equals(byte.class) || type2.equals(Byte.class);
        if (type1.equals(short.class) || type1.equals(Short.class)) return type2.equals(short.class) || type2.equals(Short.class);
        if (type1.equals(int.class) || type1.equals(Integer.class)) return type2.equals(int.class) || type2.equals(Integer.class);
        if (type1.equals(long.class) || type1.equals(Long.class)) return type2.equals(long.class) || type2.equals(Long.class);
        if (type1.equals(float.class) || type1.equals(Float.class)) return type2.equals(float.class) || type2.equals(Float.class);
        if (type1.equals(double.class) || type1.equals(Double.class)) return type2.equals(double.class) || type2.equals(Double.class);
        if (type1.equals(void.class) || type1.equals(Void.class)) return type2.equals(void.class) || type2.equals(Void.class);

        if (type1 instanceof ParameterizedType parameterizedType1 && type2 instanceof ParameterizedType parameterizedType2) {
            if (areTypesEqual(parameterizedType1.getRawType(), parameterizedType2.getRawType())) {
                var parameterTypes1 = parameterizedType1.getActualTypeArguments();
                var parameterTypes2 = parameterizedType2.getActualTypeArguments();

                if (parameterTypes1.length == parameterTypes2.length) {
                    for (int i = 0; i < parameterTypes1.length; i++) {
                        var parameterType1 = parameterTypes1[i];
                        var parameterType2 = parameterTypes2[i];

                        if (!areTypesEqual(parameterType1, parameterType2)) {
                            return false;
                        }
                    }

                    return true;
                }
            }
        }

        if (type1 instanceof GenericArrayType genericArrayType1 && type2 instanceof GenericArrayType genericArrayType2) {
            return areTypesEqual(genericArrayType1.getGenericComponentType(), genericArrayType2.getGenericComponentType());
        }

        return false;
    }

    // Helper method to check if first the given type is a instance of primaryType
    public static boolean isTypeCompatible(Type primaryType, Type type) {
        if (primaryType.equals(type)) return true;

        if (primaryType instanceof Class<?> primaryClass && type instanceof Class<?> clazz) {
            if (primaryClass.isAssignableFrom(clazz)) return true;
        }

        if (primaryType.equals(boolean.class) || primaryType.equals(Boolean.class)) return type.equals(boolean.class) || type.equals(Boolean.class);
        if (primaryType.equals(char.class) || primaryType.equals(Character.class)) return type.equals(char.class) || type.equals(Character.class);
        if (primaryType.equals(byte.class) || primaryType.equals(Byte.class)) return type.equals(byte.class) || type.equals(Byte.class);
        if (primaryType.equals(short.class) || primaryType.equals(Short.class)) return type.equals(short.class) || type.equals(Short.class);
        if (primaryType.equals(int.class) || primaryType.equals(Integer.class)) return type.equals(int.class) || type.equals(Integer.class);
        if (primaryType.equals(long.class) || primaryType.equals(Long.class)) return type.equals(long.class) || type.equals(Long.class);
        if (primaryType.equals(float.class) || primaryType.equals(Float.class)) return type.equals(float.class) || type.equals(Float.class);
        if (primaryType.equals(double.class) || primaryType.equals(Double.class)) return type.equals(double.class) || type.equals(Double.class);
        if (primaryType.equals(void.class) || primaryType.equals(Void.class)) return type.equals(void.class) || type.equals(Void.class);

        if (primaryType instanceof ParameterizedType primaryParameterizedType && type instanceof ParameterizedType parameterizedType2) {
            if (isTypeCompatible(primaryParameterizedType.getRawType(), parameterizedType2.getRawType())) {
                var primaryParameterTypes = primaryParameterizedType.getActualTypeArguments();
                var parameterTypes2 = parameterizedType2.getActualTypeArguments();

                if (primaryParameterTypes.length == parameterTypes2.length) {
                    for (int i = 0; i < primaryParameterTypes.length; i++) {
                        var primaryParameterType = primaryParameterTypes[i];
                        var parameterType2 = parameterTypes2[i];

                        if (!isTypeCompatible(primaryParameterType, parameterType2)) {
                            return false;
                        }
                    }

                    return true;
                }
            }
        }

        if (primaryType instanceof GenericArrayType primaryGenericArrayType && type instanceof GenericArrayType genericArrayType2) {
            return isTypeCompatible(primaryGenericArrayType.getGenericComponentType(), genericArrayType2.getGenericComponentType());
        }

        return false;
    }

    @Nullable
    public static Method getZeroArgMethodWithReturnType(String name, Class<?> clazz, Type returnType, Predicate<Integer> modifierChecks) {
        return getZeroArgMethodWithReturnType(name, clazz, returnType, MethodTypeCheckBypass.FALSE, modifierChecks);
    }

    @Nullable
    public static Method getZeroArgMethodWithReturnType(String name, Class<?> clazz, Type returnType, MethodTypeCheckBypass alternativeTypeCheck, Predicate<Integer> modifierChecks) {
        return getMethodWithPredicate(name, clazz, method -> {
            if (method.getParameterCount() > 0 || !modifierChecks.test(method.getModifiers())) return false;

            Type type = method.getGenericReturnType();

            // Possible candidate for generic erased type
            if (method.getReturnType().equals(Object.class) && alternativeTypeCheck.bypassTypeCheck(method)) {
                return true;
            }

            return ReflectionUtils.areTypesEqual(returnType, type);
        });
    }

    @Nullable
    public static Method getZeroArgMethodWithCompatibleReturnType(String name, Class<?> clazz, Type returnType, Predicate<Integer> modifierChecks) {
        return getZeroArgMethodWithCompatibleReturnType(name, clazz, returnType, MethodTypeCheckBypass.FALSE, modifierChecks);
    }

    @Nullable
    public static Method getZeroArgMethodWithCompatibleReturnType(String name, Class<?> clazz, Type returnType, MethodTypeCheckBypass alternativeTypeCheck, Predicate<Integer> modifierChecks) {
        return getMethodWithPredicate(name, clazz, method -> {
            if (method.getParameterCount() > 0 || !modifierChecks.test(method.getModifiers())) return false;

            Type type = method.getGenericReturnType();

            // Possible candidate for generic erased type
            if (method.getReturnType().equals(Object.class) && alternativeTypeCheck.bypassTypeCheck(method)) {
                return true;
            }

            return ReflectionUtils.isTypeCompatible(returnType, type);
        });
    }

    @Nullable
    private static Method getMethodWithSingleParameterType(String name, Class<?> clazz, Type parameterType, MethodTypeCheckBypass alternativeTypeCheck) {
        var validReturnTypes = new ArrayList<Type>(unpackClassesAndInterfaces(clazz));

        validReturnTypes.add(void.class);

        return getMethodWithPredicate(name, clazz, method -> {
            if (!Modifier.isStatic(method.getModifiers()) && validReturnTypes.stream().anyMatch(type -> areTypesEqual(type, method.getGenericReturnType()))) {
                var methodParameterTypes = method.getGenericParameterTypes();

                if (methodParameterTypes.length == 1) {
                    var methodParameterType = methodParameterTypes[0];

                    if (methodParameterType.equals(Object.class) && alternativeTypeCheck.bypassTypeCheck(method)) {
                        return true;
                    }

                    return areTypesEqual(methodParameterType, parameterType);
                }

            }

            return false;
        });
    }

    @Nullable
    public static Method getMethodWithPredicate(String name, Class<?> clazz, Predicate<Method> isValid) {
        for (var method : clazz.getMethods()) {
            if (method.getName().equals(name) && isValid.test(method)) {
                return method;
            }
        }

        return null;
    }

    public static <T> Function<T, Object> createGetter(Class<T> clazz, Field field, MethodTypeCheckBypass alternativeTypeCheck) {
//        if (Modifier.isPrivate(field.getModifiers())) {
//            var method = findGetterMethod(clazz, field, alternativeTypeCheck);
//
//            return (t) -> {
//                try { return method.invoke(t); }
//                catch (Throwable e) { throw new IllegalStateException("Unable to get field [" + field + "] value from method", e); }
//            };
//        }

        if (!field.trySetAccessible()) throw new IllegalStateException("Unable to set field Accessible: " + field);

        return (t) -> {
            try { return field.get(t); }
            catch (Throwable e) { throw new IllegalStateException("Unable to get field [" + field + "] value", e); }
        };
    }

    public static <T> BiConsumer<T, Object> createSetter(Class<T> clazz, Field field, MethodTypeCheckBypass alternativeTypeCheck) {
        if (!Modifier.isPublic(field.getModifiers()) && !field.trySetAccessible()) {
            var method = findSetterMethod(clazz, field, alternativeTypeCheck);

            return (t, o) -> {
                try { method.invoke(t, o); }
                catch (Throwable e) { throw new IllegalStateException("Unable to set field value from method", e); }
            };
        }

        return (t, o) -> {
            try { field.set(t, o); }
            catch (Throwable e) { throw new IllegalStateException("Unable to set field value", e); }
        };
    }

    private static boolean isNotStatic(int modifiers) {
        return !Modifier.isStatic(modifiers);
    }

    private static Method findGetterMethod(Class<?> clazz, Field field, MethodTypeCheckBypass alternativeTypeCheck) {
        var fieldName = field.getName();
        var method = ReflectionUtils.getZeroArgMethodWithReturnType(fieldName, clazz, field.getType(), alternativeTypeCheck, ReflectionUtils::isNotStatic);

        if (method == null) {
            var getterName = "get" + fieldName.substring(0, 1).toUpperCase(Locale.ROOT) + fieldName.substring(1);

            method = ReflectionUtils.getZeroArgMethodWithReturnType(getterName, clazz, field.getType(), alternativeTypeCheck, ReflectionUtils::isNotStatic);
        }

        if (method == null) {
            throw new IllegalStateException("Unable to get the needed getter for the following private field [" + fieldName + "] for class [" + clazz + "]");
        }

        return method;
    }

    private static Method findSetterMethod(Class<?> clazz, Field field, MethodTypeCheckBypass alternativeTypeCheck) {
        var fieldName = field.getName();
        var method = ReflectionUtils.getMethodWithSingleParameterType(fieldName, clazz, field.getType(), alternativeTypeCheck);

        if (method == null) {
            var setterName = "set" + fieldName.substring(0, 1).toUpperCase(Locale.ROOT) + fieldName.substring(1);

            method = ReflectionUtils.getMethodWithSingleParameterType(setterName, clazz, field.getType(), alternativeTypeCheck);
        }

        if (method == null) {
            throw new IllegalStateException("Unable to get the needed setter for the following private field [" + fieldName + "] for class [" + clazz + "]");
        }

        return method;
    }

    public interface AlternativeTypeGetter {
        Optional<Type> getAlternativeType(Method method);
    }
}
