package io.wispforest.endec.struct;

import io.wispforest.endec.annotations.NullableComponent;

public record TestRecord(int field1, @NullableComponent String field2, TestObject6<Integer> field3) {
}
