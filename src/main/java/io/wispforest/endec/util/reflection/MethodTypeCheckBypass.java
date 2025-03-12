package io.wispforest.endec.util.reflection;

import java.lang.reflect.Method;

public interface MethodTypeCheckBypass {

    MethodTypeCheckBypass FALSE = method -> false;

    boolean bypassTypeCheck(Method method);
}
