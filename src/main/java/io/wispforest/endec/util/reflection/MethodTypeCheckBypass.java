package io.wispforest.endec.util.reflection;

import io.wispforest.endec.impl.ReflectiveEndecBuilder;

import java.lang.reflect.Method;

///
/// Allows for the ability to tell the ReflectiveEndecBuilder if it should bypass a given [Method]('s)
/// return type check due to generic issues. Use [ReflectiveEndecBuilder#registerMethodTypeCheckBypass(Class, String...)]
/// to register such.
///
public interface MethodTypeCheckBypass {

    MethodTypeCheckBypass FALSE = method -> false;

    boolean bypassTypeCheck(Method method);
}
