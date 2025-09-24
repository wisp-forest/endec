package io.wispforest.endec.struct;

import java.util.*;

// Record like object
public class TestObject2 {
    public final boolean              field1;
    public final int                  field2;
    public final double               field3;
    public final Set<String>          field4;
    public final Map<String, Integer> field6;
    public final int[]                field7;
    public final Optional<String>     field8;
    public final TestRecord           field9;
    public final TestEnum             field10;

    private transient String field11 = "";

    public TestObject2() {
        this.field1 = false;
        this.field2 = 0;
        this.field3 = 0.0;
        this.field4 = Set.of("test");
        this.field6 = Map.of("weee", 2);
        this.field7 = new int[]{1, 2, 3};
        this.field8 = Optional.empty();
        this.field9 = new TestRecord(2, "whatever", new TestObject6<>(1));
        this.field10 = TestEnum.MAYBE;
    }

    public TestObject2(boolean field1, int field2, double field3, Set<String> field4, Map<String, Integer> field6, int[] field7, Optional<String> field8, TestRecord field9, TestEnum field10) {
        this.field1 = field1;
        this.field2 = field2;
        this.field3 = field3;
        this.field4 = field4;
        this.field6 = field6;
        this.field7 = field7;
        this.field8 = field8;
        this.field9 = field9;
        this.field10 = field10;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        TestObject2 that = (TestObject2) object;

        return field1 == that.field1
                && field2 == that.field2
                && Double.compare(field3, that.field3) == 0
                && Objects.equals(field4, that.field4)
                && Objects.equals(field6, that.field6)
                && Objects.deepEquals(field7, that.field7)
                && Objects.equals(field8, that.field8)
                && Objects.equals(field9, that.field9)
                && field10 == that.field10;
    }

    @Override
    public int hashCode() {
        return Objects.hash(field1, field2, field3, field4, field6, Arrays.hashCode(field7), field8, field9, field10);
    }
}
