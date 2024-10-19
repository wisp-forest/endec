package io.wispforest.endec;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.netty.buffer.Unpooled;
import io.wispforest.endec.format.bytebuf.ByteBufDeserializer;
import io.wispforest.endec.format.bytebuf.ByteBufSerializer;
import io.wispforest.endec.format.edm.EdmElement;
import io.wispforest.endec.format.edm.EdmSerializer;
import io.wispforest.endec.format.gson.GsonDeserializer;
import io.wispforest.endec.format.gson.GsonEndec;
import io.wispforest.endec.format.gson.GsonSerializer;
import io.wispforest.endec.util.RangeNumberException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MiscTests {

    @Test
    @DisplayName("xmap string to codepoints")
    public void xmapStringToCodePoints(){
        final var codepointEndec = Endec.SHORT.listOf()
                .xmap(
                        (shorts) -> {
                            var chars = new char[shorts.size()];

                            for (int i = 0; i < shorts.size(); i++) {
                                chars[i] = (char) (short) shorts.get(i);
                            }

                            return new String(chars);
                        },
                        (str) -> str.chars().mapToObj(value -> (Short) (short) value).toList()
                );

        final var serialized = codepointEndec.encodeFully(GsonSerializer::of, "a string");// jsonEncoder.convert(toJson(codepointEndec, ));
        System.out.println("encoded: " + serialized);
        Assertions.assertEquals(Utils.make(JsonArray::new, jsonArray -> {
            jsonArray.add(97);
            jsonArray.add(32);
            jsonArray.add(115);
            jsonArray.add(116);
            jsonArray.add(114);
            jsonArray.add(105);
            jsonArray.add(110);
            jsonArray.add(103);
        }), serialized);

        final var decoded = codepointEndec.decodeFully(GsonDeserializer::of, serialized);
        System.out.println("decoded: " + decoded);
        Assertions.assertEquals("a string", decoded);
    }

    @Test
    @DisplayName("encode json to binary")
    public void encodeJsonToBinary(){
        var json = Utils.make(JsonObject::new, jsonObject -> {
            jsonObject.addProperty("a field", "some json here");
            jsonObject.add("another_field", Utils.make(JsonArray::new, jsonArray -> {
                jsonArray.add(1.0);
                jsonArray.add(Utils.make(JsonObject::new, jsonObject1 -> {
                    jsonObject1.add("hmmm", JsonNull.INSTANCE);
                }));
            }));
        });

        var byteBuf = GsonEndec.INSTANCE.encodeFully(() -> ByteBufSerializer.of(Unpooled.buffer()), json);

        var decodedJson = GsonEndec.INSTANCE.decodeFully(ByteBufDeserializer::of, byteBuf);

        Assertions.assertEquals(json, decodedJson);
    }

    @Test
    @DisplayName("ranged nums")
    public void rangedNums(){
        Assertions.assertEquals(
                new JsonPrimitive(-2),
                Endec.clamped(Endec.INT, -2, 10).encodeFully(GsonSerializer::of, -10)
        );

        Assertions.assertEquals(
                new JsonPrimitive(10),
                Endec.clamped(Endec.INT, 0, 10).encodeFully(GsonSerializer::of, 15)
        );

        Assertions.assertThrows(RangeNumberException.class, () -> {
            Endec.ranged(Endec.FLOAT, -2f, -0.25f, true).encodeFully(GsonSerializer::of, 0.0f);
        });
    }

    @Test
    @DisplayName("attribute branching")
    public void attributeBranching(){
        final var attr1 = SerializationAttribute.marker("attr1");
        final var attr2 = SerializationAttribute.marker("attr2");

        final var endec = Endec.ifAttr(attr1, (Endec<Number>) (Object) Endec.BYTE)
                .orElseIf(attr2, (Endec<Number>) (Object)  Endec.INT)
                .orElse((Endec<Number>) (Object) Endec.LONG);

        Assertions.assertEquals(EdmElement.Type.I8, endec.encodeFully(SerializationContext.attributes(attr1), EdmSerializer::of, (byte) 16).type());
        Assertions.assertEquals(EdmElement.Type.I8, endec.encodeFully(SerializationContext.attributes(attr1, attr2), EdmSerializer::of, (byte) 16).type());
        Assertions.assertEquals(EdmElement.Type.I8, endec.encodeFully(SerializationContext.attributes(attr2, attr1), EdmSerializer::of, (byte) 16).type());
        Assertions.assertEquals(EdmElement.Type.I32,  endec.encodeFully(SerializationContext.attributes(attr2), EdmSerializer::of, 16).type());
        Assertions.assertEquals(EdmElement.Type.I64, endec.encodeFully(SerializationContext.empty(), EdmSerializer::of, (long) 16).type());

        Assertions.assertNotEquals(EdmElement.Type.I32, endec.encodeFully(SerializationContext.attributes(attr1), EdmSerializer::of, (byte) 16).type());
    }
}
