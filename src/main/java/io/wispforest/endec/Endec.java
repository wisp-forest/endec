package io.wispforest.endec;

import io.wispforest.endec.data.*;
import io.wispforest.endec.impl.*;
import io.wispforest.endec.util.QuadConsumer;
import io.wispforest.endec.util.TriConsumer;
import io.wispforest.endec.util.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;

/**
 * A combined <b>en</b>coder and <b>dec</b>oder for values of type {@code T}.
 * <p>
 * To convert between single instances of {@code T} and their serialized form,
 * use {@link #encodeFully(Supplier, ExtraDataContext, Object)} and {@link #decodeFully(Function, ExtraDataContext, Object)}
 */
public interface Endec<T> {

    /**
     * Write all data required to reconstruct {@code value} into {@code serializer}
     */
    void encode(Serializer<?> serializer, ExtraDataContext ctx, T value);

    /**
     * Decode the data specified by {@link #encode(Serializer, ExtraDataContext, Object)} and reconstruct
     * the corresponding instance of {@code T}.
     * <p>
     * Endecs which intend to handle deserialization failure by decoding a different
     * structure on error, must wrap their initial reads in a call to {@link Deserializer#tryRead(Function)}
     * to ensure that deserializer state is restored for the subsequent attempt
     */
    T decode(Deserializer<?> deserializer, ExtraDataContext ctx);

    // ---

    /**
     * Create a new serializer with result type {@code E}, call {@link #encode(Serializer, ExtraDataContext, Object)}
     * once for the provided {@code value} and return the serializer's {@linkplain Serializer#result() result}
     */
    default <E> E encodeFully(Supplier<Serializer<E>> serializerConstructor, T value, DataToken.Instance ...instances) {
        var serializer = serializerConstructor.get();

        this.encode(serializer, serializer.initalContext(ExtraDataContext.of(instances)), value);

        return serializer.result();
    }

    /**
     * Create a new deserializer by calling {@code deserializerConstructor} with {@code value}
     * and return the result of {@link #decode(Deserializer, ExtraDataContext)}
     */
    default <E> T decodeFully(Function<E, Deserializer<E>> deserializerConstructor, E value, DataToken.Instance ...instances) {
        var deserializer = deserializerConstructor.apply(value);

        return this.decode(deserializer, deserializer.initalContext(ExtraDataContext.of(instances)));
    }

    // --- Serializer Primitives ---

    Endec<Void> VOID = Endec.of((serializer, ctx, unused) -> {}, (deserializer, ctx) -> null);

    Endec<Boolean> BOOLEAN = Endec.of(Serializer::writeBoolean, Deserializer::readBoolean);
    Endec<Byte> BYTE = Endec.of(Serializer::writeByte, Deserializer::readByte);
    Endec<Short> SHORT = Endec.of(Serializer::writeShort, Deserializer::readShort);
    Endec<Integer> INT = Endec.of(Serializer::writeInt, Deserializer::readInt);
    Endec<Integer> VAR_INT = Endec.of(Serializer::writeVarInt, Deserializer::readVarInt);
    Endec<Long> LONG = Endec.of(Serializer::writeLong, Deserializer::readLong);
    Endec<Long> VAR_LONG = Endec.of(Serializer::writeVarLong, Deserializer::readVarLong);
    Endec<Float> FLOAT = Endec.of(Serializer::writeFloat, Deserializer::readFloat);
    Endec<Double> DOUBLE = Endec.of(Serializer::writeDouble, Deserializer::readDouble);
    Endec<String> STRING = Endec.of(Serializer::writeString, Deserializer::readString);
    Endec<byte[]> BYTES = Endec.of(Serializer::writeBytes, Deserializer::readBytes);

    // --- Serializer compound types ---

    /**
     * Create a new endec which serializes a list of elements
     * serialized using this endec
     */
    default Endec<List<T>> listOf() {
        return of((serializer, ctx, list) -> {
            try (var sequence = serializer.sequence(ctx, this, list.size())) {
                list.forEach(sequence::element);
            }
        }, (deserializer, ctx) -> {
            var sequenceState = deserializer.sequence(ctx, this);

            var list = new ArrayList<T>(sequenceState.estimatedSize());
            sequenceState.forEachRemaining(list::add);

            return list;
        });
    }

    /**
     * Create a new endec which serializes a map from string
     * keys to values serialized using this endec
     */
    default Endec<Map<String, T>> mapOf() {
        return of((serializer, ctx, map) -> {
            try (var mapState = serializer.map(ctx, this, map.size())) {
                map.forEach(mapState::entry);
            }
        }, (deserializer, ctx) -> {
            var mapState = deserializer.map(ctx, this);

            var map = new HashMap<String, T>(mapState.estimatedSize());
            mapState.forEachRemaining(entry -> map.put(entry.getKey(), entry.getValue()));

            return map;
        });
    }

    /**
     * Create a new endec which serializes an optional value
     * serialized using this endec
     */
    default Endec<Optional<T>> optionalOf() {
        return of(
                (serializer, ctx, value) -> serializer.writeOptional(ctx, this, value),
                (deserializer, ctx) -> deserializer.readOptional(ctx, this)
        );
    }

    // --- Constructors ---

    static <T> Endec<T> of(TriConsumer<Serializer<?>, ExtraDataContext, T> encode, BiFunction<Deserializer<?>, ExtraDataContext, T> decode) {
        return new Endec<>() {
            @Override
            public void encode(Serializer<?> serializer, ExtraDataContext ctx, T value) {
                encode.accept(serializer, ctx, value);
            }

            @Override
            public T decode(Deserializer<?> deserializer, ExtraDataContext ctx) {
                return decode.apply(deserializer, ctx);
            }
        };
    }

    /**
     * Create a new endec which serializes a map from keys serialized using
     * {@code keyEndec} to values serialized using {@code valueEndec}.
     * <p>
     * Due to the endec data model only natively supporting maps
     * with string keys, the resulting endec's serialized representation
     * is a list of key-value pairs
     */
    @SuppressWarnings("unchecked")
    static <K, V> Endec<Map<K, V>> map(Endec<K> keyEndec, Endec<V> valueEndec) {
        return StructEndecBuilder.of(
                keyEndec.fieldOf("k", Map.Entry::getKey),
                valueEndec.fieldOf("v", Map.Entry::getValue),
                Map::entry
        ).listOf().xmap(entries -> Map.ofEntries(entries.toArray(Map.Entry[]::new)), kvMap -> List.copyOf(kvMap.entrySet()));
    }

    /**
     * Create a new endec which serializes a map from keys encoded as strings using
     * {@code keyToString} and decoded using {@code stringToKey} to values serialized
     * using {@code valueEndec}
     */
    static <K, V> Endec<Map<K, V>> map(Function<K, String> keyToString, Function<String, K> stringToKey, Endec<V> valueEndec) {
        return of((serializer, ctx, map) -> {
            try (var mapState = serializer.map(ctx, valueEndec, map.size())) {
                map.forEach((k, v) -> mapState.entry(keyToString.apply(k), v));
            }
        }, (deserializer, ctx) -> {
            var mapState = deserializer.map(ctx, valueEndec);

            var map = new HashMap<K, V>(mapState.estimatedSize());
            mapState.forEachRemaining(entry -> map.put(stringToKey.apply(entry.getKey()), entry.getValue()));

            return map;
        });
    }

    /**
     * Create a new endec which serializes the enum constants of {@code enumClass}
     * <p>
     * In a human-readable format, the endec serializes to the {@linkplain Enum#name() constant's name},
     * and to its {@linkplain Enum#ordinal() ordinal} otherwise
     */
    static <E extends Enum<E>> Endec<E> forEnum(Class<E> enumClass) {
        return ifToken(
                DataTokens.HUMAN_READABLE,
                STRING.xmap(name -> Arrays.stream(enumClass.getEnumConstants()).filter(e -> e.name().equals(name)).findFirst().get(), Enum::name)
        ).orElse(
                VAR_INT.xmap(ordinal -> enumClass.getEnumConstants()[ordinal], Enum::ordinal)
        );
    }

    // ---

    /**
     * Shorthand for {@link #dispatchedStruct(Function, Function, Endec, String)}
     * which always uses {@code type} as the {@code variantKey}
     */
    static <T, K> Endec<T> dispatchedStruct(Function<K, StructEndec<? extends T>> variantToEndec, Function<T, K> instanceToVariant, Endec<K> variantEndec) {
        return dispatchedStruct(variantToEndec, instanceToVariant, variantEndec, "type");
    }

    /**
     * Create a new struct-dispatch endec which serializes variants of the struct {@code T}
     * <p>
     * To do this, it inserts an additional field given by {@code variantKey} into the beginning of the
     * struct and writes the variant identifier obtained from {@code instanceToVariant} into it
     * using {@code variantEndec}. When decoding, this variant identifier is read and the rest
     * of the struct decoded with the endec obtained from {@code variantToEndec}
     * <p>
     * For example, assume there is some interface like this
     * <pre>{@code
     * public interface Herbert {
     *      Identifier id();
     *      ... more functionality here
     * }
     * }</pre>
     *
     * which is implemented by {@code Harald} and {@code Albrecht}, whose endecs we have
     * stored in a map:
     * <pre>{@code
     * public final class Harald implements Herbert {
     *      public static final StructEndec<Harald> = StructEndecBuilder.of(...);
     *
     *      private final int haraldOMeter;
     *      ...
     * }
     *
     * public final class Albrecht implements Herbert {
     *     public static final StructEndec<Harald> = StructEndecBuilder.of(...);
     *
     *     private final List<String> dadJokes;
     *      ...
     * }
     *
     * public static final Map<Identifier, StructEndec<? extends Herbert>> HERBERT_REGISTRY = Map.of(
     *      new Identifier("herbert", "harald"), Harald.ENDEC,
     *      new Identifier("herbert", "albrecht"), Albrecht.ENDEC
     * );
     * }</pre>
     *
     * We could then create an endec capable of serializing either {@code Harald} or {@code Albrecht} as follows:
     * <pre>{@code
     * Endec.dispatchedStruct(HERBERT_REGISTRY::get, Herbert::id, BuiltInEndecs.IDENTIFIER, "type")
     * }</pre>
     *
     * If we now encode an instance of {@code Albrecht} to JSON using this endec, we'll get the following result:
     * <pre>{@code
     * {
     *      "type": "herbert:albrecht",
     *      "dad_jokes": [
     *          "What does a sprinter eat before a race? Nothing, they fast!",
     *          "Why don't eggs tell jokes? They'd crack each other up."
     *      ]
     * }
     * }</pre>
     *
     * And similarly, the following data could be used for decoding an instance of {@code Harald}:
     * <pre>{@code
     * {
     *      "type": "herbert:harald",
     *      "harald_o_meter": 69
     * }
     * }</pre>
     */
    static <T, K> Endec<T> dispatchedStruct(Function<K, StructEndec<? extends T>> variantToEndec, Function<T, K> instanceToVariant, Endec<K> variantEndec, String variantKey) {
        return new StructEndec<>() {
            @Override
            public void encodeStruct(Serializer<?> serializer, Serializer.Struct struct, ExtraDataContext ctx, T value) {
                var variant = instanceToVariant.apply(value);
                struct.field(ctx, variantKey, variantEndec, variant);

                //noinspection unchecked
                ((StructEndec<T>) variantToEndec.apply(variant)).encodeStruct(serializer, struct, ctx, value);
            }

            @Override
            public T decodeStruct(Deserializer<?> deserializer, Deserializer.Struct struct, ExtraDataContext ctx) {
                var variant = struct.field(ctx, variantKey, variantEndec);
                return variantToEndec.apply(variant).decodeStruct(deserializer, struct, ctx);
            }
        };
    }

    /**
     * Create a new dispatch endec which serializes variants of {@code T}
     * <p>
     * Such an endec is conceptually similar to a struct-dispatch one created through {@link #dispatchedStruct(Function, Function, Endec, String)}
     * (check the documentation on that function for a complete usage example), but because this family of endecs does not
     * require {@code T} to be a struct, the variant identifier field cannot be merged with the rest and is encoded separately
     */
    static <T, K> Endec<T> dispatched(Function<K, Endec<? extends T>> variantToEndec, Function<T, K> instanceToVariant, Endec<K> variantEndec) {
        return new StructEndec<>() {
            @Override
            public void encodeStruct(Serializer<?> serializer, Serializer.Struct struct, ExtraDataContext ctx, T value) {
                var variant = instanceToVariant.apply(value);
                struct.field(ctx, "variant", variantEndec, variant);

                //noinspection unchecked
                struct.field(ctx, "instance", ((Endec<T>) variantToEndec.apply(variant)), value);
            }

            @Override
            public T decodeStruct(Deserializer<?> deserializer, Deserializer.Struct struct, ExtraDataContext ctx) {
                var variant = struct.field(ctx, "variant", variantEndec);
                return struct.field(ctx, "instance", variantToEndec.apply(variant));
            }
        };
    }

    // ---

    static <T> StructEndecBranchBuilder<T> ifTokenStruct(DataToken<?> attribute, StructEndec<T> endec) {
        return new StructEndecBranchBuilder<T>().orElseIf(attribute, endec);
    }

    static <T> StructEndecBranchBuilder<T> ifTokenStruct(DataToken.Instanced<Boolean> attribute, StructEndec<T> endec) {
        return new StructEndecBranchBuilder<T>().orElseIf(attribute, endec);
    }

    static <D, T, I> StructEndecBranchBuilder<T> ifTokenStruct(DataToken.Instanced<D> attribute, StructEndec<I> endec, BiFunction<D, I, T> to, BiFunction<D, T, I> from) {
        return new StructEndecBranchBuilder<T>().orElseIf(attribute, endec, to, from);
    }

    static <T> EndecBranchBuilder<T> ifToken(DataToken<?> attribute, Endec<T> endec) {
        return new EndecBranchBuilder<T>().orElseIf(attribute, endec);
    }

    static <T> EndecBranchBuilder<T> ifToken(DataToken.Instanced<Boolean> attribute, Endec<T> endec) {
        return new EndecBranchBuilder<T>().orElseIf(attribute, endec);
    }

    static <D, T, I> EndecBranchBuilder<T> ifToken(DataToken.Instanced<D> attribute, Endec<I> endec, BiFunction<D, I, T> to, BiFunction<D, T, I> from) {
        return new EndecBranchBuilder<T>().orElseIf(attribute, endec, to, from);
    }

    //--

    default <D, R> Endec<R> ofToken(DataToken.Instanced<D> attribute, BiFunction<D, T, R> to, BiFunction<D, R, T> from) {
        return Endec.ofToken(attribute,
                (serializer, ctx, d, t) -> this.encode(serializer, ctx, from.apply(d, t)),
                (deserializer, ctx, d) -> to.apply(d, this.decode(deserializer, ctx)));
    }

    static <V, D> Endec<V> ofToken(DataToken.Instanced<D> token, QuadConsumer<Serializer<?>, ExtraDataContext, D, V> encode, TriFunction<Deserializer<?>, ExtraDataContext, D, V> decode){
        return new Endec<>() {
            @Override
            public void encode(Serializer<?> serializer, ExtraDataContext ctx, V value) {
                encode.accept(serializer, ctx, ctx.getOrThrow(token), value);
            }

            @Override
            public V decode(Deserializer<?> deserializer, ExtraDataContext ctx) {
                return decode.apply(deserializer, ctx, ctx.getOrThrow(token));
            }
        };
    }

    // --- Endec composition ---

    /**
     * Create a new endec which converts between instances of {@code T} and {@code R}
     * using {@code to} and {@code from} before encoding / after decoding
     */
    default <R> Endec<R> xmap(Function<T, R> to, Function<R, T> from) {
        return of(
                (serializer, ctx, value) -> Endec.this.encode(serializer, ctx, from.apply(value)),
                (deserializer, ctx) -> to.apply(Endec.this.decode(deserializer, ctx))
        );
    }

    /**
     * Create a new endec which runs {@code validator} (giving it the chance to throw on
     * an invalid value) before encoding / after decoding
     */
    default Endec<T> validate(Consumer<T> validator) {
        return this.xmap(t -> {
            validator.accept(t);
            return t;
        }, t -> {
            validator.accept(t);
            return t;
        });
    }

    /**
     * Create a new endec which, if decoding using this endec's {@link #decode(Deserializer, ExtraDataContext)} fails,
     * instead tries to decode using {@code decodeOnError}
     */
    default Endec<T> catchErrors(BiFunction<Deserializer<?>, Exception, T> decodeOnError) {
        return of(this::encode, (deserializer, ctx) -> {
            try {
                return deserializer.tryRead(this::decode, ctx);
            } catch (Exception e) {
                return decodeOnError.apply(deserializer, e);
            }
        });
    }

    /**
     * Create a new endec by wrapping {@link #optionalOf()} and mapping between
     * present optional &lt;-&gt; value and empty optional &lt;-&gt; null
     */
    default Endec<@Nullable T> nullableOf() {
        return this.optionalOf().xmap(o -> o.orElse(null), Optional::ofNullable);
    }

    // --- Conversion ---

    /**
     * Create a new keyed endec which (de)serializes the entry
     * with key {@code key} into/from a {@link io.wispforest.owo.serialization.util.MapCarrier},
     * decoding to {@code defaultValue} if the map does not contain such an entry
     * <p>
     * If {@code T} is of a mutable type, you almost always want to use {@link #keyed(String, Supplier)} instead
     */
    default KeyedEndec<T> keyed(String key, T defaultValue) {
        return new KeyedEndec<>(key, this, defaultValue);
    }

    /**
     * Create a new keyed endec which (de)serializes the entry
     * with key {@code key} into/from a {@link io.wispforest.owo.serialization.util.MapCarrier},
     * decoding to the result of invoking {@code defaultValueFactory} if the map does not contain such an entry
     * <p>
     * If {@code T} is of an immutable type, you almost always want to use {@link #keyed(String, Object)} instead
     */
    default KeyedEndec<T> keyed(String key, Supplier<T> defaultValueFactory) {
        return new KeyedEndec<>(key, this, defaultValueFactory);
    }

    // ---

    default <S> StructField<S, T> fieldOf(String name, Function<S, T> getter) {
        return new StructField<>(name, this, getter);
    }

    default <S> StructField<S, T> optionalFieldOf(String name, Function<S, T> getter, @Nullable T defaultValue) {
        return new StructField<>(name, this.optionalOf().xmap(optional -> optional.orElse(defaultValue), Optional::ofNullable), getter, defaultValue);
    }

    default <S> StructField<S, T> optionalFieldOf(String name, Function<S, T> getter, Supplier<@Nullable T> defaultValue) {
        return new StructField<>(name, this.optionalOf().xmap(optional -> optional.orElseGet(defaultValue), Optional::ofNullable), getter, defaultValue);
    }
}
