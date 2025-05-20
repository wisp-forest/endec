package io.wispforest.endec.util.reflection;

import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.impl.ObjectEndec;
import io.wispforest.endec.impl.RecordEndec;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;
import io.wispforest.endec.impl.StructField;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;

///
/// Allows for the ability to adjust the passed [SerializationContext] passed to the [StructField] constructed in
/// [RecordEndec] or [ObjectEndec] for each field. Such can be registered using [ReflectiveEndecBuilder#registerTypeAdjuster].
///
public interface AnnotatedContextGatherer<A extends Annotation> {
    SerializationContext getContext(AnnotatedType annotatedType, A annotation);
}
