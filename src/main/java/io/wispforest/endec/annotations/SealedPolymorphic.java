package io.wispforest.endec.annotations;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.CommentAttribute;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

///
/// An annotation to tell the [ReflectiveEndecBuilder] when looking at the given type to create an
/// [Endec] from that it can use [ReflectiveEndecBuilder#createSealedEndec] to create an [Endec] that
/// allows Encoding of all permitted classes.
///
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SealedPolymorphic {}
