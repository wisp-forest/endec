package io.wispforest.endec.struct.inheritence;

import java.util.Objects;

public class InheritedObject2 extends BaseObject2 {

    private final boolean field3;

    public InheritedObject2(String field1, int field2, boolean field3) {
        super(field1, field2);

        this.field3 = field3;
    }

    public InheritedObject2() {
        super();

        this.field3 = false;
    }

    public boolean getField3() {
        return field3;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;
        InheritedObject2 that = (InheritedObject2) object;
        return field3 == that.field3;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), field3);
    }
}
