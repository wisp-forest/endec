package io.wispforest.endec.annotations;

import io.wispforest.endec.impl.ReflectiveEndecBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

///
/// Indicates to the [ReflectiveEndecBuilder] that annotated type should be treated should be treated
/// as nullable in serialization.
///
/// Importantly, **this changes the serialized type of this component to an optional**
///
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
public @interface IsNullable {
    boolean mayOmitField();

    boolean mayOmitNullValues() default false;
}
