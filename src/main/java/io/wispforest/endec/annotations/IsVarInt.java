package io.wispforest.endec.annotations;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


///
/// Indicates to the [ReflectiveEndecBuilder] that annotated type should be treated
/// as variable variant of the [Integer] or [Long] type in serialization
/// meaning such will use either the [Endec#VAR_INT] or [Endec#VAR_LONG].
///
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
public @interface IsVarInt {
    boolean ignoreHumanReadable() default false;
}
