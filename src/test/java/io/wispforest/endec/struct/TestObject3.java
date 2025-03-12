package io.wispforest.endec.struct;

import java.util.*;

public class TestObject3 {
    public boolean              field1;
    public int                  field2;
    public double               field3;
    public Set<String>          field4;
    public Map<String, Integer> field6;
    public int[]                field7;
    public Optional<String>     field8;
    public TestRecord           field9;
    public TestEnum             field10;

    private transient String field11 = "";

    public TestObject3() {
        this.field1 = false;
        this.field2 = 0;
        this.field3 = 0.0;
        this.field4 = Set.of("test");
        this.field6 = Map.of("weee", 2);
        this.field7 = new int[]{1, 2, 3};
        this.field8 = Optional.empty();
        this.field9 = new TestRecord(2, "whatever");
        this.field10 = TestEnum.MAYBE;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        TestObject3 that = (TestObject3) object;
        return field1 == that.field1 && field2 == that.field2 && Double.compare(field3, that.field3) == 0 && Objects.equals(field4, that.field4) && Objects.equals(field6, that.field6) && Objects.deepEquals(field7, that.field7) && Objects.equals(field8, that.field8) && Objects.equals(field9, that.field9) && field10 == that.field10;
    }

    @Override
    public int hashCode() {
        return Objects.hash(field1, field2, field3, field4, field6, Arrays.hashCode(field7), field8, field9, field10);
    }
}
