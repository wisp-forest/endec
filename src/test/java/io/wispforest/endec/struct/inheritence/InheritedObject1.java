package io.wispforest.endec.struct.inheritence;

import java.util.Objects;

public class InheritedObject1 extends BaseObject1 {
    public boolean field3 = false;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;
        InheritedObject1 that = (InheritedObject1) object;
        return field3 == that.field3;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), field3);
    }
}
