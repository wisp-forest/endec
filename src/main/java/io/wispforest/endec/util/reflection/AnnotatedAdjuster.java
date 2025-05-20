package io.wispforest.endec.util.reflection;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;

///
/// Allows for the ability to adjust the passed [Endec] to either add validation, adjust decoding/encoding path
/// when building such within the [ReflectiveEndecBuilder]. Such can be registered using [ReflectiveEndecBuilder#registerTypeAdjuster].
///
public interface AnnotatedAdjuster<A extends Annotation> {
    <T> AdjustmentResult<T> adjustEndec(AnnotatedType annotatedType, A annotation, Endec<T> base);
}
