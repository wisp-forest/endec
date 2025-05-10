package io.wispforest.endec.util.reflection;

import io.wispforest.endec.SerializationContext;
import org.checkerframework.checker.units.qual.A;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;

public interface AnnotatedContextGatherer<A extends Annotation> {
    SerializationContext getContext(AnnotatedType annotatedType, A annotation);
}
