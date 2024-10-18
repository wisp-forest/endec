package io.wispforest.endec;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.wispforest.endec.format.gson.GsonSerializer;
import io.wispforest.endec.impl.StructEndecBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

public class FlatStructFieldTests {
    @Test
    @DisplayName("encode child class to json")
    public void encodeChildClassToJson(){
        final var fieldsEndec = StructEndecBuilder.of(
                Endec.STRING.fieldOf("a_field", object -> object.aField),
                Endec.INT.fieldOf("another_field", object -> object.anotherField),
                ParentData::new
        );

        final var childClassEndec = StructEndecBuilder.of(
                fieldsEndec.flatFieldOf(ParentClass::parentData),
                Endec.DOUBLE.listOf().fieldOf("third_field", object -> object.thirdField),
                (parentData, thirdField) -> new ChildClass(parentData.aField, parentData.anotherField, thirdField)
        );

        var encodedElement = childClassEndec.encodeFully(GsonSerializer::of, new ChildClass("a", 7, List.of(1.2, 2.4)));

        Assertions.assertEquals(
                Utils.make(JsonObject::new, (jsonObject) -> {
                    jsonObject.addProperty("a_field", "a");
                    jsonObject.addProperty("another_field", 7);
                    jsonObject.add("third_field", Utils.make(JsonArray::new, jsonArray -> {
                        jsonArray.add(1.2);
                        jsonArray.add(2.4);
                    }));

                }),
                encodedElement
        );
    }

    @Test
    @DisplayName("encode grandchild class to json")
    public void encodeGrandChildClassToJson(){
        final var fieldsEndec = StructEndecBuilder.of(
                Endec.STRING.fieldOf("a_field", object -> object.aField),
                Endec.INT.fieldOf("another_field", object -> object.anotherField),
                ParentData::new
        );

        final var childClassEndec = StructEndecBuilder.of(
                fieldsEndec.flatFieldOf(ParentClass::parentData),
                Endec.DOUBLE.listOf().fieldOf("third_field", object -> object.thirdField),
                (parentData, thirdField) -> new ChildClass(parentData.aField, parentData.anotherField, thirdField)
        );

        final var grandChildClassEndec = StructEndecBuilder.of(
                childClassEndec.flatInheritedFieldOf(),
                Endec.BOOLEAN.fieldOf("bruh", object -> object.bruh),
                (childClass, thirdField) -> new GrandchildClass(childClass.aField, childClass.anotherField, childClass.thirdField, thirdField)
        );

        var encodedElement = grandChildClassEndec.encodeFully(GsonSerializer::of, new GrandchildClass("b", 77, List.of(3.4, 3.5), false));

        Assertions.assertEquals(
                Utils.make(JsonObject::new, (jsonObject) -> {
                    jsonObject.addProperty("a_field", "b");
                    jsonObject.addProperty("another_field", 77);
                    jsonObject.add("third_field", Utils.make(JsonArray::new, jsonArray -> {
                        jsonArray.add(3.4);
                        jsonArray.add(3.5);
                    }));
                    jsonObject.addProperty("bruh", false);
                }),
                encodedElement
        );
    }

    public abstract static class ParentClass {
        protected final String aField;
        protected final int anotherField;

        protected ParentClass(String aField, int anotherField){
            this.aField = aField;
            this.anotherField = anotherField;
        }

        public ParentData parentData() {
            return new ParentData(this.aField, this.anotherField);
        }
    }

    public record ParentData(String aField, int anotherField){}

    public static class ChildClass extends ParentClass {
        private final List<Double> thirdField;

        protected ChildClass(String aField, int anotherField, List<Double> thirdField){
            super(aField, anotherField);

            this.thirdField = thirdField;
        }
    }

    public static class GrandchildClass extends ChildClass {
        private final boolean bruh;

        protected GrandchildClass(String aField, int anotherField, List<Double> thirdField, boolean bruh) {
            super(aField, anotherField, thirdField);

            this.bruh = bruh;
        }
    }
}
