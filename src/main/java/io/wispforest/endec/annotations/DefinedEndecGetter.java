package io.wispforest.endec.annotations;


import io.wispforest.endec.impl.ReflectiveEndecBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

///
/// An annotation that tells the [ReflectiveEndecBuilder] that given Field or Method attached is
/// an Endec for the given class such exists within
///
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DefinedEndecGetter {
}
