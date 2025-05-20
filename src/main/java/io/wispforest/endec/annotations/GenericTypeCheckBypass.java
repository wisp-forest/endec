package io.wispforest.endec.annotations;

import io.wispforest.endec.impl.ReflectiveEndecBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
///
/// An annotation to tell the [ReflectiveEndecBuilder] to bypass type checks when looking for methods
/// for the given type object
///
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GenericTypeCheckBypass {
    String[] methodsToBypass();
}
