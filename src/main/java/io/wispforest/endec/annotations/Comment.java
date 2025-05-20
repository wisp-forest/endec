package io.wispforest.endec.annotations;

import io.wispforest.endec.impl.CommentAttribute;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

///
/// An annotation to tell the [ReflectiveEndecBuilder] when looking at the field to add a comment as
/// [CommentAttribute] for a given context object passed
///
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Comment {
    String comment();
}
