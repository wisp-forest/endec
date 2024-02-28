package io.wispforest.endec.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates to the {@link io.wispforest.endec.impl.RecordEndec} that this record component
 * should be treated as nullable in serialization. Importantly, <b>this changes the serialized type of this
 * component to an optional</b>
 */
@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.RUNTIME)
public @interface NullableComponent {}
