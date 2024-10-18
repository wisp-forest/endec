package io.wispforest.endec;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.wispforest.endec.format.gson.GsonDeserializer;
import io.wispforest.endec.format.gson.GsonEndec;
import io.wispforest.endec.format.gson.GsonSerializer;
import io.wispforest.endec.impl.StructEndecBuilder;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonTests {

    @Test
    @DisplayName("encode string")
    public void encodeString(){
        var value = "an epic string";
        var result = Endec.STRING.encodeFully(GsonSerializer::of, value);
        System.out.println("Result: " + result + ", Type: " + result.getClass());
    }

    @Test
    @DisplayName("encode struct")
    public void encodeStruct(){
        var endec = StructEndecBuilder.of(
                Endec.STRING.fieldOf("a_field", StructObject::aField),
                Endec.STRING.mapOf().fieldOf("a_nested_field", StructObject::aNestedField),
                Endec.DOUBLE.listOf().fieldOf("list_moment", StructObject::listMoment),
                Endec.STRING.fieldOf("another_field", StructObject::anotherField),
                StructObject::new
        );

        var structObject = new StructObject(
                "an epic field value",
                Utils.make(LinkedHashMap::new, map -> {
                    map.put("a", "bruh");
                    map.put("b", "nested field value, epic");
                }),
                List.of(1.0, 5.7, Double.MAX_VALUE),
                "this too"
        );

        var encodedElement = endec.encodeFully(GsonSerializer::of, structObject);

        Assertions.assertEquals(
                Utils.make(JsonObject::new, (jsonObject) -> {
                    jsonObject.addProperty("a_field", "an epic field value");
                    jsonObject.add("a_nested_field", Utils.make(JsonObject::new, jsonObject1 -> {
                        jsonObject1.addProperty("a", "bruh");
                        jsonObject1.addProperty("b", "nested field value, epic");
                    }));
                    jsonObject.add("list_moment", Utils.make(JsonArray::new, jsonArray -> {
                        jsonArray.add(1.0);
                        jsonArray.add(5.7);
                        jsonArray.add(Double.MAX_VALUE);
                    }));
                    jsonObject.addProperty("another_field", "this too");
                }),
                encodedElement
        );

        var decodedValue = endec.decodeFully(GsonDeserializer::of, encodedElement);

        Assertions.assertEquals(structObject, decodedValue);
    }

    @Test
    @DisplayName("encode json to json")
    public void encodeJsonToJson(){
        var json = Utils.make(
                JsonObject::new, jsonObject -> {
                    jsonObject.addProperty("a field", "some json here");
                    jsonObject.add("another field", Utils.make(JsonArray::new, (jsonArray) -> {
                        jsonArray.add(1.0);
                        jsonArray.add(Utils.make(JsonObject::new, jsonObject1 -> {
                            jsonObject1.add("hmmm", JsonNull.INSTANCE);
                        }));
                    }));
                }
        );

        var encoded = GsonEndec.INSTANCE.encodeFully(GsonSerializer::of, json);
        Assertions.assertEquals(json, encoded);
    }

    @Test
    @DisplayName("omit optional field during encoding / read default during decoding")
    public void optionalFieldHandling(){
        var endec = StructEndecBuilder.of(Endec.INT.optionalFieldOf("field", SingleInteger::integer, () -> 0), SingleInteger::new);

        Assertions.assertEquals(new JsonObject(), endec.encodeFully(GsonSerializer::of, new SingleInteger(null)));

        Assertions.assertEquals(new SingleInteger(0), endec.decodeFully(GsonDeserializer::of, new JsonObject()));
    }

    public record SingleInteger(@Nullable Integer integer){}

    public record StructObject(String aField, Map<String, String> aNestedField, List<Double> listMoment, String anotherField) {}
}
