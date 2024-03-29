package io.wispforest.endec;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import io.wispforest.endec.format.edm.*;
import io.wispforest.endec.impl.EitherEndec;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class CodecUtils {

    /**
     * Create a new endec serializing the same data as {@code codec}
     * <p>
     * This method is implemented by converting all data to be (de-)serialized
     * to the Endec Data Model data format (hereto-forth to be referred to as EDM)
     * which has both an endec ({@link EdmEndec}) and DynamicOps implementation ({@link EdmOps}).
     * Since EDM encodes structure using a self-described format's native structural types,
     * <b>this means that for JSON and NBT, the created endec's serialized representation is identical
     * to that of {@code codec}</b>. In general, for non-self-described formats, the serialized
     * representation is a byte array
     * <p>
     * When decoding, an EDM element is read from the deserializer and then parsed using {@code codec}
     * <p>
     * When encoding, the value is encoded using {@code codec} to an EDM element which is then
     * written into the serializer
     */
    public static <T> Endec<T> ofCodec(Codec<T> codec) {
        return Endec.of(
                (serializer, value) -> {
                    EdmEndec.INSTANCE.encode(serializer, getResult(codec.encodeStart(EdmOps.create(serializer), value), IllegalStateException::new));
                },
                deserializer -> {
                    return getResult(codec.parse(EdmOps.create(deserializer), EdmEndec.INSTANCE.decode(deserializer)), IllegalStateException::new);
                }
        );
    }

    /**
     * Create a codec serializing the same data as this endec, assuming
     * that the serialized format posses the {@code assumedAttributes}
     * <p>
     * This method is implemented by converting between a given DynamicOps'
     * datatype and EDM (see {@link #ofCodec(Codec)}) and then encoding/decoding
     * from/to an EDM element using the {@link EdmSerializer} and {@link EdmDeserializer}
     * <p>
     * The serialized representation of a codec created through this method is generally
     * identical to that of a codec manually created to describe the same data
     */
    public static <T> Codec<T> codec(Endec<T> endec, DataTokenHolder<?>... assumedTokens) {
        return new Codec<>() {
            @Override
            public <D> DataResult<Pair<T, D>> decode(DynamicOps<D> ops, D input) {
                try {
                    var edmElement = ops.convertTo(EdmOps.INSTANCE, input);

                    var deserializer = LenientEdmDeserializer.of(edmElement);

                    if(ops instanceof ExtraDataContext context) deserializer.gatherFrom(context);

                    return DataResult.success(new Pair<>(endec.decode(deserializer.withTokens(assumedTokens)), input));
                } catch (Exception e) {
                    return DataResult.error(e::getMessage);
                }
            }

            @Override
            public <D> DataResult<D> encode(T input, DynamicOps<D> ops, D prefix) {
                try {
                    var serializer = EdmSerializer.of();

                    if(ops instanceof ExtraDataContext context) serializer.gatherFrom(context);

                    return DataResult.success(EdmOps.INSTANCE.convertTo(ops, endec.encodeFully(() -> serializer.withTokens(assumedTokens), input)));
                } catch (Exception e) {
                    return DataResult.error(e::getMessage);
                }
            }
        };
    }

    public static <T> MapCodec<T> mapCodec(StructEndec<T> structEndec, DataTokenHolder<?>... assumedTokens) {
        return new MapCodec<>() {
            @Override
            public <T1> Stream<T1> keys(DynamicOps<T1> ops) {
                throw new UnsupportedOperationException("MapCodec generated from StructEndec cannot report keys");
            }

            @Override
            public <T1> DataResult<T> decode(DynamicOps<T1> ops, MapLike<T1> input) {
                try {
                    var map = new HashMap<String, EdmElement<?>>();
                    input.entries().forEach(pair -> {
                        map.put(
                                getResult(
                                        ops.getStringValue(pair.getFirst()),
                                        s -> new IllegalStateException("Unable to parse key: " + s)
                                ),
                                ops.convertTo(EdmOps.INSTANCE, pair.getSecond())
                        );
                    });

                    var edmElement = EdmElement.wrapMap(map);

                    var deserializer = LenientEdmDeserializer.of(edmElement);

                    if(ops instanceof ExtraDataContext context) deserializer.gatherFrom(context);

                    return DataResult.success(structEndec.decode(deserializer.withTokens(assumedTokens)));
                } catch (Exception e) {
                    return DataResult.error(e::getMessage);
                }
            }

            @Override
            public <T1> RecordBuilder<T1> encode(T input, DynamicOps<T1> ops, RecordBuilder<T1> prefix) {
                try {
                    var serializer = EdmSerializer.of();

                    if(ops instanceof ExtraDataContext context) serializer.gatherFrom(context);

                    var element = structEndec.encodeFully(() -> serializer.withTokens(assumedTokens), input).<Map<String, EdmElement<?>>>cast();

                    var result = prefix;
                    for (var entry : element.entrySet()) {
                        result = result.add(entry.getKey(), EdmOps.INSTANCE.convertTo(ops, entry.getValue()));
                    }

                    return result;
                } catch (Exception e) {
                    return prefix.withErrorsFrom(DataResult.error(e::getMessage, input));
                }
            }
        };
    }

    // ---

    /**
     * Create an endec which serializes an instance of {@link Either}, using {@code first}
     * for the left and {@code second} for the right variant
     * <p>
     * In a self-describing format, the serialized representation is simply that of the endec of
     * whichever variant is represented. In the general for non-self-described formats, the
     * which variant is represented must also be stored
     */
    public static <F, S> Endec<Either<F, S>> either(Endec<F> first, Endec<S> second) {
        return new EitherEndec<>(first, second, false);
    }

    /**
     * Like {@link #either(Endec, Endec)}, but ensures when decoding from a self-described format
     * that only {@code first} or {@code second}, but not both, succeed
     */
    public static <F, S> Endec<Either<F, S>> xor(Endec<F> first, Endec<S> second) {
        return new EitherEndec<>(first, second, true);
    }

    // ---

    public static <T, E extends Throwable> T getResult(DataResult<T> result, Function<String, E> exceptionGetter) throws E {
        Optional<DataResult.PartialResult<T>> optional = result.error();
        if (optional.isPresent()) {
            throw exceptionGetter.apply((optional.get()).message());
        } else {
            return (T)result.result().orElseThrow();
        }
    }
}
