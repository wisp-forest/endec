package io.wispforest.endec;

import io.wispforest.endec.annotations.DefinedEndecGetter;
import io.wispforest.endec.format.edm.EdmDeserializer;
import io.wispforest.endec.format.edm.EdmSerializer;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.endec.struct.*;
import io.wispforest.endec.struct.inheritence.ImmutablePairTest;
import io.wispforest.endec.struct.inheritence.InheritedObject1;
import io.wispforest.endec.struct.inheritence.InheritedObject2;
import io.wispforest.endec.struct.inheritence.InheritedObject3;
import io.wispforest.endec.util.reflection.ReflectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

public class ReflectionTests {

    private static final ReflectiveEndecBuilder BUILDER = new ReflectiveEndecBuilder();

    @Test
    @DisplayName("test DefinedEndec Annotation")
    public void testDeprecatedNullable(){
        Endec<TestRecord> endec;

        try {
            endec = BUILDER.get(TestRecord.class);
        } catch (Throwable e) {
            Assertions.fail(e);

            return;
        }

        var obj = new TestRecord(0, null);

        var element = endec.encodeFully(SerializationContext.empty(), EdmSerializer::of, obj);

        var newObj = endec.decodeFully(SerializationContext.empty(), EdmDeserializer::of, element);

        Assertions.assertEquals(obj, newObj);
    }

    @Test
    @DisplayName("all public fields with default no arg constructor")
    public void allPublicFieldsTest(){
        Endec<TestObject1> endec;

        try {
            endec = BUILDER.get(TestObject1.class);
        } catch (Throwable e) {
            Assertions.fail(e);

            return;
        }

        var obj = new TestObject1();

        var element = endec.encodeFully(SerializationContext.empty(), EdmSerializer::of, obj);

        var newObj = endec.decodeFully(SerializationContext.empty(), EdmDeserializer::of, element);

        Assertions.assertEquals(obj, newObj);
    }

    @Test
    @DisplayName("all public fields with no arg constructor")
    public void allPublicFieldsTestWithNoArg(){
        Endec<TestObject3> endec;

        try {
            endec = BUILDER.get(TestObject3.class);
        } catch (Throwable e) {
            Assertions.fail(e);

            return;
        }

        var obj = new TestObject3();

        var element = endec.encodeFully(SerializationContext.empty(), EdmSerializer::of, obj);

        var newObj = endec.decodeFully(SerializationContext.empty(), EdmDeserializer::of, element);

        Assertions.assertEquals(obj, newObj);
    }

    @Test
    @DisplayName("all final fields like record")
    public void allPublicFinalFieldsRecord(){
        Endec<TestObject2> endec;

        try {
            endec = BUILDER.get(TestObject2.class);
        } catch (Throwable e) {
            Assertions.fail(e);

            return;
        }

        var obj = new TestObject2();

        var element = endec.encodeFully(SerializationContext.empty(), EdmSerializer::of, obj);

        var newObj = endec.decodeFully(SerializationContext.empty(), EdmDeserializer::of, element);

        Assertions.assertEquals(obj, newObj);
    }

    @Test
    @DisplayName("pair implemented object with private fields")
    public void pairImplementedObjectWithPrivateFields(){
        Endec<TestObject4> endec;

        try {
            endec = BUILDER.get(TestObject4.class);
        } catch (Throwable e) {
            Assertions.fail(e);

            return;
        }

        var obj = new TestObject4("test", false);

        var element = endec.encodeFully(SerializationContext.empty(), EdmSerializer::of, obj);

        var newObj = endec.decodeFully(SerializationContext.empty(), EdmDeserializer::of, element);

        Assertions.assertEquals(obj, newObj);
    }

    @Test
    @DisplayName("pair like object with private fields")
    public void pairLikeObjectWithPrivateFields(){
        Endec<TestObject5> endec;

        try {
            endec = BUILDER.get(TestObject5.class);
        } catch (Throwable e) {
            Assertions.fail(e);

            return;
        }

        var obj = new TestObject5("test", false);

        var element = endec.encodeFully(SerializationContext.empty(), EdmSerializer::of, obj);

        var newObj = endec.decodeFully(SerializationContext.empty(), EdmDeserializer::of, element);

        Assertions.assertEquals(obj, newObj);
    }

    //--

    @Test
    @DisplayName("inherited object with default NoArg constructor")
    public void inheritedObjectWithDefaultNoArg(){
        Endec<InheritedObject1> endec;

        try {
            endec = BUILDER.get(InheritedObject1.class);
        } catch (Throwable e) {
            Assertions.fail(e);

            return;
        }

        var obj = new InheritedObject1();

        var element = endec.encodeFully(SerializationContext.empty(), EdmSerializer::of, obj);

        var newObj = endec.decodeFully(SerializationContext.empty(), EdmDeserializer::of, element);

        Assertions.assertEquals(obj, newObj);
    }

    @Test
    @DisplayName("inherited object with default Designated Constructor")
    public void inheritedObjectWithDesignatedConstructor(){
        Endec<InheritedObject2> endec;

        try {
            endec = BUILDER.get(InheritedObject2.class);
        } catch (Throwable e) {
            Assertions.fail(e);

            return;
        }

        var obj = new InheritedObject2();

        var element = endec.encodeFully(SerializationContext.empty(), EdmSerializer::of, obj);

        var newObj = endec.decodeFully(SerializationContext.empty(), EdmDeserializer::of, element);

        Assertions.assertEquals(obj, newObj);
    }

    @Test
    @DisplayName("inherited object with generic info")
    public void inheritedObjectWithGenericInfo(){
        Endec<InheritedObject3<String, Boolean>> endec;

        BUILDER.registerMethodTypeCheckBypass(InheritedObject3.class, "left", "right");

        try {
            endec = (Endec<InheritedObject3<String, Boolean>>) BUILDER.get(ReflectionUtils.createParameterizedType(InheritedObject3.class, String.class, Boolean.class));
        } catch (Throwable e) {
            Assertions.fail(e);

            return;
        }

        var obj = new InheritedObject3<>("test", false);

        var element = endec.encodeFully(SerializationContext.empty(), EdmSerializer::of, obj);

        var newObj = endec.decodeFully(SerializationContext.empty(), EdmDeserializer::of, element);

        Assertions.assertEquals(obj, newObj);
    }

    @Test
    @DisplayName("record with generic info")
    public void recordWithGenericInfo(){
        Endec<ImmutablePairTest<String, Boolean>> endec;

        try {
            endec = (Endec<ImmutablePairTest<String, Boolean>>) BUILDER.get(ReflectionUtils.createParameterizedType(ImmutablePairTest.class, String.class, Boolean.class));
        } catch (Throwable e) {
            Assertions.fail(e);

            return;
        }

        var obj = new ImmutablePairTest<>("test", false);

        var element = endec.encodeFully(SerializationContext.empty(), EdmSerializer::of, obj);

        var newObj = endec.decodeFully(SerializationContext.empty(), EdmDeserializer::of, element);

        Assertions.assertEquals(obj, newObj);
    }

    @Test
    @DisplayName("test DefinedEndec Annotation")
    public void testDefinedEndecAnnotation(){
        Endec<Funny> endec;

        try {
            endec = BUILDER.get(Funny.class);
        } catch (Throwable e) {
            Assertions.fail(e);

            return;
        }

        var obj = new Funny();

        var element = endec.encodeFully(SerializationContext.empty(), EdmSerializer::of, obj);

        var newObj = endec.decodeFully(SerializationContext.empty(), EdmDeserializer::of, element);

        Assertions.assertEquals(obj, newObj);
    }

    @DefinedEndecGetter
    public static class Funny {

        private String name;
        private int numberOfFunny;
        private boolean isReallyFunny;

        public Funny() {
            this("unknown", -1, false);
        }

        public Funny(String name, int numberOfFunny, boolean isReallyFunny) {
            this.name = name;
            this.numberOfFunny = numberOfFunny;
            this.isReallyFunny = isReallyFunny;
        }

        public static Endec<Funny> getEndec() {
            return StructEndecBuilder.of(
                    Endec.STRING.fieldOf("unknown", s -> s.name),
                    Endec.INT.fieldOf("numberOfFunny", s -> s.numberOfFunny),
                    Endec.BOOLEAN.fieldOf("isReallyFunny", s -> s.isReallyFunny),
                    Funny::new
            );
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            Funny funny = (Funny) object;
            return numberOfFunny == funny.numberOfFunny && isReallyFunny == funny.isReallyFunny && Objects.equals(name, funny.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, numberOfFunny, isReallyFunny);
        }
    }
}
