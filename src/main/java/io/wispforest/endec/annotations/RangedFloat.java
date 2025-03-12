package io.wispforest.endec.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RangedFloat {
    double min() default -Double.MAX_VALUE;
    double max() default Double.MAX_VALUE;
    boolean throwError() default false;
}
