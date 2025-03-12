package io.wispforest.endec.struct.inheritence;

import java.util.Objects;

public abstract class BaseObject2 {
    private final String field1;
    private final int field2;

    public BaseObject2(String field1, int field2) {
        this.field1 = field1;
        this.field2 = field2;
    }

    public BaseObject2() {
        this("", 1);
    }

    public String getField1() {
        return field1;
    }

    public int getField2() {
        return field2;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        BaseObject2 that = (BaseObject2) object;
        return field2 == that.field2 && Objects.equals(field1, that.field1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field1, field2);
    }
}
