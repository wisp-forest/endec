package io.wispforest.endec.annotations;

import io.wispforest.endec.impl.ReflectiveEndecBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

///
/// An annotation to tell the [ReflectiveEndecBuilder] when looking at the field to restrict the given Endec
/// to a [#min()] or [#max()] value either by clamping such or throwing an error when outside the given range.
///
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RangedInteger {
    long min() default -Long.MAX_VALUE;
    long max() default Long.MAX_VALUE;
    boolean throwError() default false;
}
