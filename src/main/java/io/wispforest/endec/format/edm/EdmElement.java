package io.wispforest.endec.format.edm;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.wispforest.endec.util.BlockWriter;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public sealed class EdmElement<T> permits EdmMap {

    public static final EdmElement<Optional<EdmElement<?>>> EMPTY = new EdmElement<>(Optional.empty(), Type.OPTIONAL);

    private final T value;
    private final Type type;

    EdmElement(T value, Type type) {
        this.value = value;
        this.type = type;
    }

    public T value() {
        return this.value;
    }

    @SuppressWarnings("unchecked")
    public <V> V cast() {
        return (V) this.value;
    }

    public Type type() {
        return this.type;
    }

    public Object unwrap() {
        if (this.value instanceof List<?> list) {
            return list.stream().map(o -> ((EdmElement<?>) o).unwrap()).toList();
        } else if (this.value instanceof Map<?, ?> map) {
            return map.entrySet().stream().map(entry -> Map.entry(entry.getKey(), ((EdmElement<?>) entry.getValue()).unwrap())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else if (this.value instanceof Optional<?> optional) {
            return optional.map(o -> ((EdmElement<?>) o).unwrap());
        } else {
            return this.value;
        }
    }

    /**
     * Create a copy of this EDM element as an {@link EdmMap}, which
     * implements the {@link io.wispforest.endec.util.MapCarrier} interface
     */
    public EdmMap asMap() {
        if(this.type != Type.MAP) {
            throw new IllegalStateException("Cannot cast EDM element of type " + this.type + " to MAP");
        }

        return new EdmMap(new HashMap<>(this.<Map<String, EdmElement<?>>>cast()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EdmElement<?> that)) return false;
        if (!this.value.equals(that.value)) return false;
        return this.type == that.type;
    }

    @Override
    public int hashCode() {
        int result = this.value.hashCode();
        result = 31 * result + this.type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return format(new BlockWriter()).buildResult();
    }

    protected BlockWriter format(BlockWriter formatter) {
        return switch (this.type){
            case BYTES -> {
                yield formatter.writeBlock("bytes(", ")", false, blockWriter -> {
                    blockWriter.write(Arrays.toString(Base64.getEncoder().encode(this.<byte[]>cast())));
                });
            }
            case MAP -> {
                yield formatter.writeBlock("map({", "})", blockWriter -> {
                    var map = this.<Map<String, EdmElement<?>>>cast();

                    int idx = 0;

                    for (var entry : map.entrySet()) {
                        formatter.write("\"" + entry.getKey() + "\": ");
                        entry.getValue().format(formatter);

                        if (idx < map.size() - 1) formatter.writeln(",");

                        idx++;
                    }
                });
            }
            case SEQUENCE -> {
                yield formatter.writeBlock("sequence([", "])", blockWriter -> {
                    var list = this.<List<EdmElement<?>>>cast();

                    for (int idx = 0; idx < list.size(); idx++) {
                        list.get(idx).format(formatter);
                        if (idx < list.size() - 1) formatter.writeln(",");
                    }
                });
            }
            case OPTIONAL -> {
                yield formatter.writeBlock("optional(", ")", false, blockWriter -> {
                    var optional = this.<Optional<EdmElement<?>>>cast();

                    optional.ifPresentOrElse(
                            edmElement -> edmElement.format(formatter),
                            () -> formatter.write(""));
                });
            }
            case STRING -> {
                yield formatter.writeBlock("string(\"", "\")", false, blockWriter -> {
                    blockWriter.write(Objects.toString(value));
                });
            }
            default -> {
                yield formatter.writeBlock(type.formatName() + "(", ")", false, blockWriter -> {
                    blockWriter.write(Objects.toString(value));
                });
            }
        };
    }

    public static EdmElement<Byte> i8(byte value) {
        return new EdmElement<>(value, Type.I8);
    }

    public static EdmElement<Byte> u8(byte value) {
        return new EdmElement<>(value, Type.U8);
    }

    public static EdmElement<Short> i16(short value) {
        return new EdmElement<>(value, Type.I16);
    }

    public static EdmElement<Short> u16(short value) {
        return new EdmElement<>(value, Type.U16);
    }

    public static EdmElement<Integer> i32(int value) {
        return new EdmElement<>(value, Type.I32);
    }

    public static EdmElement<Integer> u32(int value) {
        return new EdmElement<>(value, Type.U32);
    }

    public static EdmElement<Long> i64(long value) {
        return new EdmElement<>(value, Type.I64);
    }

    public static EdmElement<Long> u64(long value) {
        return new EdmElement<>(value, Type.U64);
    }

    public static EdmElement<Float> f32(float value) {
        return new EdmElement<>(value, Type.F32);
    }

    public static EdmElement<Double> f64(double value) {
        return new EdmElement<>(value, Type.F64);
    }

    public static EdmElement<Boolean> bool(boolean value) {
        return new EdmElement<>(value, Type.BOOLEAN);
    }

    public static EdmElement<String> string(String value) {
        return new EdmElement<>(value, Type.STRING);
    }

    public static EdmElement<byte[]> bytes(byte[] value) {
        return new EdmElement<>(value, Type.BYTES);
    }

    public static EdmElement<Optional<EdmElement<?>>> optional(@Nullable EdmElement<?> value) {
        return optional(Optional.ofNullable(value));
    }

    public static EdmElement<Optional<EdmElement<?>>> optional(Optional<EdmElement<?>> value) {
        if(value.isEmpty()) return EdmElement.EMPTY;

        return new EdmElement<>(value, Type.OPTIONAL);
    }

    public static EdmElement<List<EdmElement<?>>> sequence(List<EdmElement<?>> value) {
        return new EdmElement<>(ImmutableList.copyOf(value), Type.SEQUENCE);
    }

    public static EdmElement<Map<String, EdmElement<?>>> map(Map<String, EdmElement<?>> value) {
        return new EdmElement<>(ImmutableMap.copyOf(value), Type.MAP);
    }

    public static EdmElement<Map<String, EdmElement<?>>> consumeMap(Map<String, EdmElement<?>> value) {
        return new EdmElement<>(Collections.unmodifiableMap(value), Type.MAP); // Hangry
    }

    public enum Type {
        I8,
        U8,
        I16,
        U16,
        I32,
        U32,
        I64,
        U64,
        F32,
        F64,

        BOOLEAN,
        STRING,
        BYTES,
        OPTIONAL,

        SEQUENCE,
        MAP;

        public String formatName(){
            return this.name().toLowerCase();
        }
    }
}
