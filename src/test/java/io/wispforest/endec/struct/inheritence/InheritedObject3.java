package io.wispforest.endec.struct.inheritence;

import io.wispforest.endec.annotations.GenericTypeCheckBypass;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;

@GenericTypeCheckBypass(methodsToBypass = {"left", "right"})
public class InheritedObject3<K, V> extends ObjectObjectMutablePair<K, V> {
    /**
     * Creates a new type-specific mutable {@link Pair Pair} with given left and
     * right value.
     *
     * @param left  the left value.
     * @param right the right value.
     */
    public InheritedObject3(K left, V right) {
        super(left, right);
    }

    /*
     * Due to type erasure the types on the left and right methods are just Object
     * meaning there is no way to recover info on how to find the real type if need be.
     *
     * Fixing this would require that API dedicated to allowing the ability to check a method
     * based on its name and if
     * - The given return type is Object and there are no method parameters
     * - The given return type is void and there is only one method parameter as Object
     *
     * Such would allow any instance of this generic type within a record component or field declaration
     * to have an endec created reflectively.
     */
}
