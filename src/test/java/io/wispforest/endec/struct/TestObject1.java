package io.wispforest.endec.struct;

import java.util.*;

public class TestObject1 {
    public boolean              field1 = false;
    public int                  field2 = 0;
    public double               field3 = 0.0;
    public Set<String>          field4 = Set.of("test");
    public Map<String, Integer> field6 = Map.of("weee", 2);
    public int[]                field7 = new int[]{1, 2, 3};
    public Optional<String>     field8 = Optional.empty();
    public TestRecord           field9 = new TestRecord(2, "whatever");
    public TestEnum             field10 = TestEnum.MAYBE;

    private transient String field11 = "";

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        TestObject1 that = (TestObject1) object;

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
