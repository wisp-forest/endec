package io.wispforest.endec.annotations;

import io.wispforest.endec.Endec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates to the {@link io.wispforest.endec.impl.RecordEndec} that this record component
 * should be treated as variable variant of the {@link Integer} or {@link Long} type in serialization
 * meaning such will use either the {@link Endec#VAR_INT} or {@link Endec#VAR_LONG}.
 */
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
public @interface IsVarInt {
    boolean ignoreHumanReadable() default false;
}
