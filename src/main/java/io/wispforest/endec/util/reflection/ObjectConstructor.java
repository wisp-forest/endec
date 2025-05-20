package io.wispforest.endec.util.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

///
/// A constructor for a given object with the type [T] typically using a [Constructor] for reflective construction
/// of an object.
///
public interface ObjectConstructor<T> {

    static <T> ObjectConstructor<T> fromConstructor(Constructor<T> constructor) {
        return initargs -> {
            try {
                return (T) constructor.newInstance(initargs);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException | IllegalArgumentException e) {
                throw new IllegalStateException("Error while deserializing object [" + constructor.getDeclaringClass() + "]", e);
            }
        };
    }

    T createInstance(Object... initargs);
}
