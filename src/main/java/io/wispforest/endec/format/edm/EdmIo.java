package io.wispforest.endec.format.edm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

public class EdmIo {

    public static void encode(DataOutput output, EdmElement<?> data) throws IOException {
        output.writeByte(data.type().ordinal());
        encodeElementData(output, data);
    }

    public static EdmElement<?> decode(DataInput input) throws IOException {
        return decodeElementData(input, input.readByte());
    }

    public static void encodeElementData(DataOutput output, EdmElement<?> data) throws IOException {
        switch (data.type()) {
            case I8, U8 -> output.writeByte(data.<Byte>cast());
            case I16, U16 -> output.writeShort(data.<Short>cast());
            case I32, U32 -> output.writeInt(data.cast());
            case I64, U64 -> output.writeLong(data.cast());
            case F32 -> output.writeFloat(data.cast());
            case F64 -> output.writeDouble(data.cast());
            case BOOLEAN -> output.writeBoolean(data.cast());
            case STRING -> output.writeUTF(data.cast());
            case BYTES -> {
                output.writeInt((data.<byte[]>cast()).length);
                output.write(data.cast());
            }
            case OPTIONAL -> {
                var optional = data.<Optional<EdmElement<?>>>cast();

                output.writeBoolean(optional.isPresent());
                if (optional.isPresent()) {
                    var element = optional.get();

                    output.writeByte(element.type().ordinal());
                    encodeElementData(output, element);
                }
            }
            case SEQUENCE -> {
                var list = data.<List<EdmElement<?>>>cast();

                output.writeInt(list.size());
                if (!list.isEmpty()) {
                    for (var element : list) {
                        output.writeByte(element.type().ordinal());
                        encodeElementData(output, element);
                    }
                }
            }
            case MAP -> {
                var map = data.<Map<String, EdmElement<?>>>cast();

                output.writeInt(map.size());
                for (var entry : map.entrySet()) {
                    output.writeUTF(entry.getKey());

                    output.writeByte(entry.getValue().type().ordinal());
                    encodeElementData(output, entry.getValue());
                }
            }
        }
    }

    private static EdmElement<?> decodeElementData(DataInput input, byte type) throws IOException {
        return switch (EdmElement.Type.values()[type]) {
            case I8 -> EdmElement.i8(input.readByte());
            case U8 -> EdmElement.u8(input.readByte());
            case I16 -> EdmElement.i16(input.readShort());
            case U16 -> EdmElement.u16(input.readShort());
            case I32 -> EdmElement.i32(input.readInt());
            case U32 -> EdmElement.u32(input.readInt());
            case I64 -> EdmElement.i64(input.readLong());
            case U64 -> EdmElement.u64(input.readLong());
            case F32 -> EdmElement.f32(input.readFloat());
            case F64 -> EdmElement.f64(input.readDouble());
            case BOOLEAN -> EdmElement.bool(input.readBoolean());
            case STRING -> EdmElement.string(input.readUTF());
            case BYTES -> {
                var result = new byte[input.readInt()];
                input.readFully(result);

                yield EdmElement.bytes(result);
            }
            case OPTIONAL -> {
                if (input.readByte() != 0) {
                    yield EdmElement.optional(Optional.of(decodeElementData(input, input.readByte())));
                } else {
                    yield EdmElement.optional(Optional.empty());
                }
            }
            case SEQUENCE -> {
                var length = input.readInt();
                if (length != 0) {
                    var result = new ArrayList<EdmElement<?>>(length);

                    for (int i = 0; i < length; i++) {
                        result.add(decodeElementData(input, input.readByte()));
                    }

                    yield EdmElement.sequence(result);
                } else {
                    yield EdmElement.sequence(List.of());
                }
            }
            case MAP -> {
                var length = input.readInt();
                var result = new LinkedHashMap<String, EdmElement<?>>(length);

                for (int i = 0; i < length; i++) {
                    result.put(
                            input.readUTF(),
                            decodeElementData(input, input.readByte())
                    );
                }

                yield EdmElement.consumeMap(result);
            }
        };
    }

}
