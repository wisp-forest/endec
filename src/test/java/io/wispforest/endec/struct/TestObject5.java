package io.wispforest.endec.struct;

import java.util.Objects;

public class TestObject5 {

    protected String left;
    protected boolean right;

    public TestObject5(final String left, final boolean right) {
        this.left = left;
        this.right = right;
    }

    public String getLeft() {
        return left;
    }

    public TestObject5 setLeft(String l) {
        left = l;
        return this;
    }

    public Boolean getRight() {
        return right;
    }

    public TestObject5 setRight(Boolean r) {
        right = r;
        return this;
    }

    @Override
    public String toString() {
        return "<" + getLeft() + "," + getLeft() + ">";
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        TestObject5 that = (TestObject5) object;
        return getRight() == that.getRight() && Objects.equals(getLeft(), that.getLeft());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLeft(), getRight());
    }
}
