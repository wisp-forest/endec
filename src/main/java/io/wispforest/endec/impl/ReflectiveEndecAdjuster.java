package io.wispforest.endec.impl;

import io.wispforest.endec.Endec;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * Functional interface allowing for the adjustments of to be returned for
 * @param <A>
 */
public interface ReflectiveEndecAdjuster<A extends Annotation> {

    @Nullable <T> Endec<T> adjustEndec(AnnotatedElement element, A annotationInstance, Endec<T> base);
}
