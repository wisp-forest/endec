package io.wispforest.endec.util.reflection;

import io.wispforest.endec.Endec;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;

public interface AnnotatedAdjuster<A extends Annotation> {
    <T> AdjustmentResult<T> adjustEndec(AnnotatedType annotatedType, A annotation, Endec<T> base);
}
