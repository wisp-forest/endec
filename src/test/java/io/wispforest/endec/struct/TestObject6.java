package io.wispforest.endec.struct;

import java.util.Objects;

public class TestObject6<T> {
    public T field1;

    public TestObject6(T field1) {
        this.field1 = field1;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TestObject6<?> that)) return false;
        return Objects.equals(field1, that.field1);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(field1);
    }
}
