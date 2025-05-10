package io.wispforest.endec.struct.inheritence;

import java.util.Objects;

public abstract class BaseObject1 {
    public String field1 = "weee";
    public int field2 = 190;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        BaseObject1 that = (BaseObject1) object;
        return field2 == that.field2 && Objects.equals(field1, that.field1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field1, field2);
    }
}
