package io.wispforest.endec;

import com.google.common.io.ByteStreams;
import io.wispforest.endec.format.edm.EdmDeserializer;
import io.wispforest.endec.format.edm.EdmElement;
import io.wispforest.endec.format.edm.EdmIo;
import io.wispforest.endec.format.edm.EdmSerializer;
import io.wispforest.endec.impl.StructEndecBuilder;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EdmTests {

    @Test
    @DisplayName("toString Formatting")
    public void toStringFormatting(){
        var edmElement = EdmElement.wrapMap(
                Utils.make(LinkedHashMap::new, innerMap -> {
                    innerMap.put("ah_yes", EdmElement.wrapSequence(List.of(EdmElement.wrapInt(17), EdmElement.wrapString("a"))));
                    innerMap.put("hmmm", EdmElement.wrapOptional(Optional.empty()));
                    innerMap.put("uhhh", EdmElement.wrapOptional(
                            EdmElement.wrapMap(
                                    Utils.make(LinkedHashMap::new, map -> {
                                        map.put("b", EdmElement.wrapOptional(EdmElement.wrapFloat(16.5f)));
                                    })
                            )
                    ));
                })
        );

        Assertions.assertEquals(
                """
                map({
                  ah_yes: sequence([
                    i32(17),
                    string(a)
                  ]),
                  hmmm: optional(),
                  uhhh: optional(map({
                    b: optional(f32(16.5))
                  }))
                })""",
                edmElement.toString()
        );
    }

    @Test
    @DisplayName("struct encode")
    public void structEncode(){
        final StructEndec<Triple<List<Integer>, @Nullable String, Map<String, @Nullable Float>>> endec = StructEndecBuilder.of(
                Endec.INT.listOf().fieldOf("ah_yes", Triple::a),
                Endec.STRING.nullableOf().fieldOf("hmmm", Triple::b),
                Endec.FLOAT.nullableOf().mapOf().nullableOf().fieldOf("uhhh", Triple::c),
                Triple::new
        );

        var value = new Triple<>(List.of(34, 35), "test", Map.of("b", 16.5f));

        var originalEdmElement = EdmElement.wrapMap(
                Utils.make(LinkedHashMap::new, innerMap -> {
                    innerMap.put("ah_yes", EdmElement.wrapSequence(List.of(EdmElement.wrapInt(34), EdmElement.wrapInt(35))));
                    innerMap.put("hmmm", EdmElement.wrapOptional(Optional.of(EdmElement.wrapString("test"))));
                    innerMap.put("uhhh", EdmElement.wrapOptional(
                            EdmElement.wrapMap(
                                    Utils.make(LinkedHashMap::new, map -> {
                                        map.put("b", EdmElement.wrapOptional(EdmElement.wrapFloat(16.5f)));
                                    })
                            )
                    ));
                })
        );

        var encodedElement = endec.encodeFully(EdmSerializer::of, value);

        Assertions.assertEquals(originalEdmElement, encodedElement);
    }

    @Test
    @DisplayName("struct decode")
    public void structDecode(){
        final StructEndec<Triple<List<Integer>, @Nullable String, Map<String, @Nullable Float>>> endec = StructEndecBuilder.of(
                Endec.INT.listOf().fieldOf("ah_yes", Triple::a),
                Endec.STRING.nullableOf().fieldOf("hmmm", Triple::b),
                Endec.FLOAT.nullableOf().mapOf().nullableOf().fieldOf("uhhh", Triple::c),
                Triple::new
        );

        var edmElement = EdmElement.wrapMap(
                Map.of(
                        "ah_yes", EdmElement.wrapSequence(List.of(EdmElement.wrapInt(34), EdmElement.wrapInt(35))),
                        "hmmm", EdmElement.wrapOptional(Optional.empty()),
                        "uhhh", EdmElement.wrapOptional(EdmElement.wrapMap(Map.of("b", EdmElement.wrapOptional(EdmElement.wrapFloat(16.5f)))))
                )
        );

        var decodedValue = endec.decodeFully(EdmDeserializer::of, edmElement);

        Assertions.assertEquals(decodedValue.a(), List.of(34, 35));
        Assertions.assertNull(decodedValue.b());
        Assertions.assertEquals(decodedValue.c(), Map.of("b", 16.5f));
    }

    public record Triple<A, B, C>(A a, B b, C c){}

    @Test
    @DisplayName("edm encode / decode")
    public void edmEncodeAndDecode(){
        var edmElement = EdmElement.wrapMap(
                Utils.make(LinkedHashMap::new, innerMap -> {
                    innerMap.put("ah_yes", EdmElement.wrapSequence(List.of(EdmElement.wrapInt(17), EdmElement.wrapString("a"))));
                    innerMap.put("hmmm", EdmElement.wrapOptional(Optional.of(EdmElement.wrapString("test"))));
                    innerMap.put("uhhh",
                            EdmElement.wrapOptional(
                                    EdmElement.wrapMap(
                                            Utils.make(LinkedHashMap::new, map -> {
                                                map.put("b", EdmElement.wrapOptional(EdmElement.wrapFloat(16.5f)));
                                            })
                                    )
                            )
                    );
                })
        );

        Assertions.assertEquals(edmElement, decodeEdmElement(encodeEdmElement(edmElement)));
    }

    private static byte[] encodeEdmElement(EdmElement<?> edmElement) {
        try {
            var dataOutput = ByteStreams.newDataOutput();

            EdmIo.encode(dataOutput, edmElement);

            return dataOutput.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static EdmElement<?> decodeEdmElement(byte[] bytes) {
        try {
            var dataInput = ByteStreams.newDataInput(bytes);

            return EdmIo.decode(dataInput);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("bytes formatting")
    public void bytesFormatting() {
        System.out.println(EdmElement.wrapBytes(new byte[]{1, 2, 4, 8, 16}));
    }
}
