package io.wispforest.endec.impl;

import io.wispforest.endec.Endec;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;

/**
 * Functional interface allowing for the adjustments of to be returned for
 * @param <A>
 */
public interface TypedAdjuster<A extends Annotation> {
    @Nullable <T> Endec<T> adjustEndec(AnnotatedType annotatedType, A annotationInstance, @Nullable Endec<T> base);
}
