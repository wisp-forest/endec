package io.wispforest.endec.struct;

import it.unimi.dsi.fastutil.Pair;

import java.util.Objects;

public class TestObject4 implements Pair<String, Boolean> {

    protected String left;
    protected boolean right;

    public TestObject4(final String left, final boolean right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String left() {
        return left;
    }

    @Override
    public TestObject4 left(String l) {
        left = l;
        return this;
    }

    @Override
    public Boolean right() {
        return right;
    }

    @Override
    public TestObject4 right(Boolean r) {
        right = r;
        return this;
    }

    @Override
    public String toString() {
        return "<" + left() + "," + right() + ">";
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        TestObject4 that = (TestObject4) object;
        return right == that.right && Objects.equals(left, that.left);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }
}
