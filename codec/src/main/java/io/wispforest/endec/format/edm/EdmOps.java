package io.wispforest.endec.format.edm;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.wispforest.endec.DataToken;
import io.wispforest.endec.DataTokenHolder;
import io.wispforest.endec.ExtraDataContext;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class EdmOps implements DynamicOps<EdmElement<?>>, ExtraDataContext {

    public static final EdmOps INSTANCE = new EdmOps();

    private EdmOps() {}

    public static EdmOps create(ExtraDataContext context) {
        return new EdmOps().gatherFrom(context);
    }

    private final java.util.Map<DataToken<?>, Object> contextData = new HashMap<>();

    @Override
    public Set<DataTokenHolder<?>> allTokens() {
        return this.contextData.entrySet().stream()
                .map(entry -> entry.getKey().holderFromUnsafe(entry.getValue()))
                .collect(Collectors.toSet());
    }

    @Override
    @Nullable
    public <DATA_TYPE> DATA_TYPE get(DataToken<DATA_TYPE> token) {
        return (DATA_TYPE) this.contextData.get(token);
    }

    @Override
    public <DATA_TYPE> void set(DataToken<DATA_TYPE> token, DATA_TYPE data) {
        this.contextData.put(token, data);
    }

    @Override
    public <DATA_TYPE> boolean has(DataToken<DATA_TYPE> token) {
        return this.contextData.containsKey(token);
    }

    @Override
    public <DATA_TYPE> void remove(DataToken<DATA_TYPE> token) {
        this.contextData.remove(token);
    }

    // --- Serialization ---

    @Override
    public EdmElement<?> empty() {
        return null;
    }

    public EdmElement<?> createNumeric(Number number) {
        return EdmElement.wrapDouble(number.doubleValue());
    }

    public EdmElement<?> createByte(byte b) {
        return EdmElement.wrapByte(b);
    }

    public EdmElement<?> createShort(short s) {
        return EdmElement.wrapShort(s);
    }

    public EdmElement<?> createInt(int i) {
        return EdmElement.wrapInt(i);
    }

    public EdmElement<?> createLong(long l) {
        return EdmElement.wrapLong(l);
    }

    public EdmElement<?> createFloat(float f) {
        return EdmElement.wrapFloat(f);
    }

    public EdmElement<?> createDouble(double d) {
        return EdmElement.wrapDouble(d);
    }

    // ---

    public EdmElement<?> createBoolean(boolean bl) {
        return EdmElement.wrapBoolean(bl);
    }

    @Override
    public EdmElement<?> createString(String value) {
        return EdmElement.wrapString(value);
    }

    @Override
    public EdmElement<?> createByteList(ByteBuffer input) {
        return EdmElement.wrapBytes(DataFixUtils.toArray(input));
    }

    // ---

    @Override
    public EdmElement<?> createList(Stream<EdmElement<?>> input) {
        return EdmElement.wrapSequence(input.toList());
    }

    @Override
    public DataResult<EdmElement<?>> mergeToList(EdmElement<?> list, EdmElement<?> value) {
        if (list == null) {
            return DataResult.success(EdmElement.wrapSequence(List.of(value)));
        } else if (list.value() instanceof List<?> properList) {
            var newList = new ArrayList<EdmElement<?>>((Collection<? extends EdmElement<?>>) properList);
            newList.add(value);

            return DataResult.success(EdmElement.wrapSequence(newList));
        } else {
            return DataResult.error(() -> "Not a sequence: " + list);
        }
    }

    @Override
    public EdmElement<?> createMap(Stream<Pair<EdmElement<?>, EdmElement<?>>> map) {
        return EdmElement.wrapMap(map.collect(Collectors.toMap(pair -> pair.getFirst().cast(), Pair::getSecond)));
    }

    @Override
    public DataResult<EdmElement<?>> mergeToMap(EdmElement<?> map, EdmElement<?> key, EdmElement<?> value) {
        if (!(key.value() instanceof String)) {
            return DataResult.error(() -> "Key is not a string: " + key);
        }

        if (map == null) {
            return DataResult.success(EdmElement.wrapMap(Map.of(key.cast(), value)));
        } else if (map.value() instanceof Map<?, ?> properMap) {
            var newMap = new HashMap<String, EdmElement<?>>((Map<String, ? extends EdmElement<?>>) properMap);
            newMap.put(key.cast(), value);

            return DataResult.success(EdmElement.wrapMap(newMap));
        } else {
            return DataResult.error(() -> "Not a map: " + map);
        }
    }

    // --- Deserialization ---

    @Override
    public DataResult<Number> getNumberValue(EdmElement<?> input) {
        if (input.value() instanceof Number number) {
            return DataResult.success(number);
        } else {
            return DataResult.error(() -> "Not a number: " + input);
        }
    }

    @Override
    public DataResult<Boolean> getBooleanValue(EdmElement<?> input) {
        if (input.value() instanceof Boolean b) {
            return DataResult.success(b);
        } else {
            return DataResult.error(() -> "Not a boolean: " + input);
        }
    }

    @Override
    public DataResult<String> getStringValue(EdmElement<?> input) {
        if (input.value() instanceof String string) {
            return DataResult.success(string);
        } else {
            return DataResult.error(() -> "Not a string: " + input);
        }
    }

    @Override
    public DataResult<ByteBuffer> getByteBuffer(EdmElement<?> input) {
        if (input.value() instanceof byte[] bytes) {
            return DataResult.success(ByteBuffer.wrap(bytes));
        } else {
            return DataResult.error(() -> "Not bytes: " + input);
        }
    }

    // ---

    @Override
    public DataResult<Stream<EdmElement<?>>> getStream(EdmElement<?> input) {
        if (input.value() instanceof List<?> list) {
            return DataResult.success(list.stream().map(o -> (EdmElement<?>) o));
        } else {
            return DataResult.error(() -> "Not a sequence: " + input);
        }
    }

    @Override
    public DataResult<Stream<Pair<EdmElement<?>, EdmElement<?>>>> getMapValues(EdmElement<?> input) {
        if (input.value() instanceof Map<?, ?> map) {
            //noinspection rawtypes
            return DataResult.success(map.entrySet().stream().map(entry -> new Pair(EdmElement.wrapString((String) entry.getKey()), entry.getValue())));
        } else {
            return DataResult.error(() -> "Not a map: " + input);
        }
    }

    // ---

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, EdmElement<?> input) {
        return switch (input.type()) {
            case BYTE -> outOps.createByte(input.cast());
            case SHORT -> outOps.createShort(input.cast());
            case INT -> outOps.createInt(input.cast());
            case LONG -> outOps.createLong(input.cast());
            case FLOAT -> outOps.createFloat(input.cast());
            case DOUBLE -> outOps.createDouble(input.cast());
            case BOOLEAN -> outOps.createBoolean(input.cast());
            case STRING -> outOps.createString(input.cast());
            case BYTES -> outOps.createByteList(ByteBuffer.wrap(input.cast()));
            case OPTIONAL ->
                    input.<Optional<EdmElement<?>>>cast().map(element -> this.convertTo(outOps, element)).orElse(outOps.empty());
            case SEQUENCE ->
                    outOps.createList(input.<List<EdmElement<?>>>cast().stream().map(element -> this.convertTo(outOps, element)));
            case MAP ->
                    outOps.createMap(input.<Map<String, EdmElement<?>>>cast().entrySet().stream().map(entry -> new Pair<>(outOps.createString(entry.getKey()), this.convertTo(outOps, entry.getValue()))));
        };
    }

    @Override
    public EdmElement<?> remove(EdmElement<?> input, String key) {
        if (input.value() instanceof Map<?, ?> map) {
            var newMap = new HashMap<String, EdmElement<?>>((Map<? extends String, ? extends EdmElement<?>>) map);
            newMap.remove(key);

            return EdmElement.wrapMap(newMap);
        } else {
            return input;
        }
    }
}
