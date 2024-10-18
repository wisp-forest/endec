package io.wispforest.endec;

import io.wispforest.endec.impl.*;
import io.wispforest.endec.util.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;

/**
 * A combined <b>en</b>coder and <b>dec</b>oder for values of type {@code T}.
 * <p>
 * To convert between single instances of {@code T} and their serialized form,
 * use {@link #encodeFully(SerializationContext, Supplier, Object)} and {@link #decodeFully(SerializationContext, Function, Object)}
 */
public interface Endec<T> {

    /**
     * Write all data required to reconstruct {@code value} into {@code serializer}
     */
    void encode(SerializationContext ctx, Serializer<?> serializer, T value);

    /**
     * Decode the data specified by {@link #encode(SerializationContext, Serializer, Object)} and reconstruct
     * the corresponding instance of {@code T}.
     * <p>
     * Endecs which intend to handle deserialization failure by decoding a different
     * structure on error, must wrap their initial reads in a call to {@link Deserializer#tryRead(Function)}
     * to ensure that deserializer state is restored for the subsequent attempt
     */
    T decode(SerializationContext ctx, Deserializer<?> deserializer);

    // ---

    /**
     * Create a new serializer with result type {@code E}, call {@link #encode(SerializationContext, Serializer, Object)}
     * once for the provided {@code value} and return the serializer's {@linkplain Serializer#result() result}
     */
    default <E> E encodeFully(SerializationContext ctx, Supplier<Serializer<E>> serializerConstructor, T value) {
        var serializer = serializerConstructor.get();
        this.encode(serializer.setupContext(ctx), serializer, value);

        return serializer.result();
    }

    default <E> E encodeFully(Supplier<Serializer<E>> serializerConstructor, T value) {
        return encodeFully(SerializationContext.empty(), serializerConstructor, value);
    }

    /**
     * Create a new deserializer by calling {@code deserializerConstructor} with {@code value}
     * and return the result of {@link #decode(SerializationContext, Deserializer)}
     */
    default <E> T decodeFully(SerializationContext ctx, Function<E, Deserializer<E>> deserializerConstructor, E value) {
        var deserializer = deserializerConstructor.apply(value);
        return this.decode(deserializer.setupContext(ctx), deserializer);
    }

    default <E> T decodeFully(Function<E, Deserializer<E>> deserializerConstructor, E value) {
        return decodeFully(SerializationContext.empty(), deserializerConstructor, value);
    }

    // --- Serializer Primitives ---

    Endec<Void> VOID = Endec.of((ctx, serializer, unused) -> {}, (ctx, deserializer) -> null);

    Endec<Boolean> BOOLEAN = Endec.of((ctx, serializer, value) -> serializer.writeBoolean(ctx, value), (ctx, deserializer) -> deserializer.readBoolean(ctx));
    Endec<Byte> BYTE = Endec.of((ctx, serializer, value) -> serializer.writeByte(ctx, value), (ctx, deserializer) -> deserializer.readByte(ctx));
    Endec<Short> SHORT = Endec.of((ctx, serializer, value) -> serializer.writeShort(ctx, value), (ctx, deserializer) -> deserializer.readShort(ctx));
    Endec<Integer> INT = Endec.of((ctx, serializer, value) -> serializer.writeInt(ctx, value), (ctx, deserializer) -> deserializer.readInt(ctx));
    Endec<Integer> VAR_INT = Endec.of((ctx, serializer, value) -> serializer.writeVarInt(ctx, value), (ctx, deserializer) -> deserializer.readVarInt(ctx));
    Endec<Long> LONG = Endec.of((ctx, serializer, value) -> serializer.writeLong(ctx, value), (ctx, deserializer) -> deserializer.readLong(ctx));
    Endec<Long> VAR_LONG = Endec.of((ctx, serializer, value) -> serializer.writeVarLong(ctx, value), (ctx, deserializer) -> deserializer.readVarLong(ctx));
    Endec<Float> FLOAT = Endec.of((ctx, serializer, value) -> serializer.writeFloat(ctx, value), (ctx, deserializer) -> deserializer.readFloat(ctx));
    Endec<Double> DOUBLE = Endec.of((ctx, serializer, value) -> serializer.writeDouble(ctx, value), (ctx, deserializer) -> deserializer.readDouble(ctx));
    Endec<String> STRING = Endec.of((ctx, serializer, value) -> serializer.writeString(ctx, value), (ctx, deserializer) -> deserializer.readString(ctx));
    Endec<byte[]> BYTES = Endec.of((ctx, serializer, value) -> serializer.writeBytes(ctx, value), (ctx, deserializer) -> deserializer.readBytes(ctx));

    // --- Serializer compound types ---

    /**
     * Create a new endec which serializes a list of elements
     * serialized using this endec
     */
    default Endec<List<T>> listOf() {
        return of((ctx, serializer, list) -> {
            try (var sequence = serializer.sequence(ctx, this, list.size())) {
                list.forEach(sequence::element);
            }
        }, (ctx, deserializer) -> {
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
        return of((ctx, serializer, map) -> {
            try (var mapState = serializer.map(ctx, this, map.size())) {
                map.forEach(mapState::entry);
            }
        }, (ctx, deserializer) -> {
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
                (ctx, serializer, value) -> serializer.writeOptional(ctx, this, value),
                (ctx, deserializer) -> deserializer.readOptional(ctx, this)
        );
    }

    // --- Constructors ---

    static <T> Endec<T> of(Encoder<T> encoder, Decoder<T> decoder) {
        return new Endec<>() {
            @Override
            public void encode(SerializationContext ctx, Serializer<?> serializer, T value) {
                encoder.encode(ctx, serializer, value);
            }

            @Override
            public T decode(SerializationContext ctx, Deserializer<?> deserializer) {
                return decoder.decode(ctx, deserializer);
            }
        };
    }

    static <T> StructEndec<T> unit(T instance) {
        return unit(() -> instance);
    }

    static <T> StructEndec<T> unit(Supplier<T> instance) {
        return StructEndec.of((ctx, serializer, struct, value) -> {}, (ctx, deserializer, struct) -> instance.get());
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
        return of((ctx, serializer, map) -> {
            try (var mapState = serializer.map(ctx, valueEndec, map.size())) {
                map.forEach((k, v) -> mapState.entry(keyToString.apply(k), v));
            }
        }, (ctx, deserializer) -> {
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
        return ifAttr(
                SerializationAttributes.HUMAN_READABLE,
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
            public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, T value) {
                var variant = instanceToVariant.apply(value);
                struct.field(variantKey, ctx, variantEndec, variant);

                //noinspection unchecked
                ((StructEndec<T>) variantToEndec.apply(variant)).encodeStruct(ctx, serializer, struct, value);
            }

            @Override
            public T decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
                var variant = struct.field(variantKey, ctx, variantEndec);
                return variantToEndec.apply(variant).decodeStruct(ctx, deserializer, struct);
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
            public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, T value) {
                var variant = instanceToVariant.apply(value);
                struct.field("variant", ctx, variantEndec, variant);

                //noinspection unchecked
                struct.field("instance", ctx, ((Endec<T>) variantToEndec.apply(variant)), value);
            }

            @Override
            public T decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
                var variant = struct.field("variant", ctx, variantEndec);
                return struct.field("instance", ctx, variantToEndec.apply(variant));
            }
        };
    }

    // ---

    static <T> AttributeEndecBuilder<T> ifAttr(SerializationAttribute attribute, Endec<T> endec) {
        return new AttributeEndecBuilder<>(endec, attribute);
    }

    // --- Endec composition ---

    /**
     * Create a new endec which converts between instances of {@code T} and {@code R}
     * using {@code to} and {@code from} before encoding / after decoding
     */
    default <R> Endec<R> xmap(Function<T, R> to, Function<R, T> from) {
        return of(
                (ctx, serializer, value) -> Endec.this.encode(ctx, serializer, from.apply(value)),
                (ctx, deserializer) -> to.apply(Endec.this.decode(ctx, deserializer))
        );
    }

    /**
     * Create a new endec which converts between instances of {@code T} and {@code R}
     * using {@code to} and {@code from} before encoding / after decoding, optionally using
     * the current {@linkplain SerializationContext serialization context}
     */
    default <R> Endec<R> xmapWithContext(BiFunction<SerializationContext, T, R> to, BiFunction<SerializationContext, R, T> from) {
        return of(
                (ctx, serializer, value) -> Endec.this.encode(ctx, serializer, from.apply(ctx, value)),
                (ctx, deserializer) -> to.apply(ctx, Endec.this.decode(ctx, deserializer))
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

    static <N extends Number & Comparable<N>> Endec<N> clampedMax(Endec<N> endec, N max) {
        return clamped(endec, null, max);
    }

    static <N extends Number & Comparable<N>> Endec<N> rangedMax(Endec<N> endec, N max, boolean throwError) {
        return ranged(endec, null, max, throwError);
    }

    static <N extends Number & Comparable<N>> Endec<N> clampedMin(Endec<N> endec, N min) {
        return clamped(endec, min, null);
    }

    static <N extends Number & Comparable<N>> Endec<N> rangedMin(Endec<N> endec, N min, boolean throwError) {
        return ranged(endec, min, null, throwError);
    }

    static <N extends Number & Comparable<N>> Endec<N> clamped(Endec<N> endec, @Nullable N min, @Nullable N max) {
        return ranged(endec, min, max, false);
    }

    static <N extends Number & Comparable<N>> Endec<N> ranged(Endec<N> endec, @Nullable N min, @Nullable N max, boolean throwError) {
        Function<N, N> errorChecker = n -> {
            // 1st check if the given min value exist and then compare similar to: [n < min]
            // 2nd check if the given min value exist and then compare similar to: [n > max]
            if (min != null && n.compareTo(min) < 0) {
                if(throwError) throw new RangeNumberException(n, min, max);
                return min;
            } else if (max != null && n.compareTo(max) > 0) {
                if(throwError) throw new RangeNumberException(n, min, max);
                return max;
            }
            return n;
        };

        return endec.xmap(errorChecker::apply, errorChecker::apply);
    }

    /**
     * Create a new endec which, if decoding using this endec's {@link #decode(SerializationContext, Deserializer)} fails,
     * instead tries to decode using {@code decodeOnError}
     */
    default Endec<T> catchErrors(DecoderWithError<T> decodeOnError) {
        return of(this::encode, (ctx, deserializer) -> {
            try {
                return deserializer.tryRead(deserializer1 -> this.decode(ctx, deserializer1));
            } catch (Exception e) {
                return decodeOnError.decode(ctx, deserializer, e);
            }
        });
    }

    /**
     * Create a new endec which serializes a set of elements
     * serialized using this endec as an xmapped list
     */
    default Endec<Set<T>> setOf() {
        return this.listOf().xmap(HashSet::new, ArrayList::new);
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
     * with key {@code key} into/from a {@link MapCarrier},
     * decoding to {@code defaultValue} if the map does not contain such an entry
     * <p>
     * If {@code T} is of a mutable type, you almost always want to use {@link #keyed(String, Supplier)} instead
     */
    default KeyedEndec<T> keyed(String key, T defaultValue) {
        return new KeyedEndec<>(key, this, defaultValue);
    }

    /**
     * Create a new keyed endec which (de)serializes the entry
     * with key {@code key} into/from a {@link MapCarrier},
     * decoding to the result of invoking {@code defaultValueFactory} if the map does not contain such an entry
     * <p>
     * If {@code T} is of an immutable type, you almost always want to use {@link #keyed(String, Object)} instead
     */
    default KeyedEndec<T> keyed(String key, Supplier<T> defaultValueFactory) {
        return new KeyedEndec<>(key, this, defaultValueFactory);
    }

    // ---

    default StructEndec<T> structOf(String name) {
        return StructEndec.of(
                (ctx, serializer, struct, value) -> struct.field(name, ctx, Endec.this, value),
                (ctx, serializer, struct) -> struct.field(name, ctx, Endec.this));
    }

    default <S> StructField<S, T> fieldOf(String name, Function<S, T> getter) {
        return new StructField<>(name, this, getter);
    }

    default <S> StructField<S, T> optionalFieldOf(String name, Function<S, T> getter, @Nullable T defaultValue) {
        return new StructField<>(name, this.optionalOf().xmap(optional -> optional.orElse(defaultValue), Optional::ofNullable), getter, defaultValue);
    }

    default <S> StructField<S, T> optionalFieldOf(String name, Function<S, T> getter, Supplier<@Nullable T> defaultValue) {
        return new StructField<>(name, this.optionalOf().xmap(optional -> optional.orElseGet(defaultValue), Optional::ofNullable), getter, defaultValue);
    }

    @FunctionalInterface
    interface Encoder<T> {
        void encode(SerializationContext ctx, Serializer<?> serializer, T value);
    }

    @FunctionalInterface
    interface Decoder<T> {
        T decode(SerializationContext ctx, Deserializer<?> deserializer);
    }

    @FunctionalInterface
    interface DecoderWithError<T> {
        T decode(SerializationContext ctx, Deserializer<?> serializer, Exception exception);
    }
}
