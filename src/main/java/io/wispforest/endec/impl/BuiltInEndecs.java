package io.wispforest.endec.impl;

import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationAttribute;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;

public final class BuiltInEndecs{

    private BuiltInEndecs() {}

    // --- Java Types ---

    public static final Endec<int[]> INT_ARRAY = Endec.INT.listOf().xmap((list) -> list.stream().mapToInt(v -> v).toArray(), (ints) -> Arrays.stream(ints).boxed().toList());
    public static final Endec<long[]> LONG_ARRAY = Endec.LONG.listOf().xmap((list) -> list.stream().mapToLong(v -> v).toArray(), (longs) -> Arrays.stream(longs).boxed().toList());

    public static final Endec<BitSet> BITSET = LONG_ARRAY.xmap(BitSet::valueOf, BitSet::toLongArray);

    public static final Endec<java.util.UUID> UUID = Endec
            .ifAttr(
                    SerializationAttribute.HUMAN_READABLE,
                    Endec.STRING.xmap(java.util.UUID::fromString, java.util.UUID::toString)
            ).orElse(
                    INT_ARRAY.xmap(BuiltInEndecs::toUuid, BuiltInEndecs::toIntArray)
            );

    public static final Endec<Date> DATE = Endec
            .ifAttr(
                    SerializationAttribute.HUMAN_READABLE,
                    Endec.STRING.xmap(s -> Date.from(Instant.parse(s)), date -> date.toInstant().toString())
            ).orElse(
                    Endec.LONG.xmap(Date::new, Date::getTime)
            );

    private static java.util.UUID toUuid(int[] array) {
        return new UUID((long)array[0] << 32 | (long)array[1] & 4294967295L, (long)array[2] << 32 | (long)array[3] & 4294967295L);
    }

    private static int[] toIntArray(UUID uuid) {
        return toIntArray(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    private static int[] toIntArray(long uuidMost, long uuidLeast) {
        return new int[]{(int)(uuidMost >> 32), (int)uuidMost, (int)(uuidLeast >> 32), (int)uuidLeast};
    }

    // --- MC Types ---
//
//    public static final Endec<Identifier> IDENTIFIER = Endec.STRING.xmap(Identifier::new, Identifier::toString);
//    public static final Endec<ItemStack> ITEM_STACK = NbtEndec.COMPOUND.xmap(ItemStack::fromNbt, stack -> stack.writeNbt(new NbtCompound()));
//    public static final Endec<Text> TEXT = Endec.ofCodec(TextCodecs.CODEC);
//
//    public static final Endec<Vec3i> VEC3I = vectorEndec("Vec3i", Endec.INT, Vec3i::new, Vec3i::getX, Vec3i::getY, Vec3i::getZ);
//    public static final Endec<Vec3d> VEC3D = vectorEndec("Vec3d", Endec.DOUBLE, Vec3d::new, Vec3d::getX, Vec3d::getY, Vec3d::getZ);
//    public static final Endec<Vector3f> VECTOR3F = vectorEndec("Vector3f", Endec.FLOAT, Vector3f::new, Vector3f::x, Vector3f::y, Vector3f::z);
//
//    public static final Endec<BlockPos> BLOCK_POS = Endec
//            .ifAttr(
//                    SerializationAttribute.HUMAN_READABLE,
//                    vectorEndec("BlockPos", Endec.INT, BlockPos::new, BlockPos::getX, BlockPos::getY, BlockPos::getZ)
//            ).orElse(
//                    Endec.LONG.xmap(BlockPos::fromLong, BlockPos::asLong)
//            );
//
//    public static final Endec<ChunkPos> CHUNK_POS = Endec
//            .ifAttr(
//                    SerializationAttribute.HUMAN_READABLE,
//                    Endec.INT.listOf().validate(ints -> {
//                        if (ints.size() != 2) {
//                            throw new IllegalStateException("ChunkPos array must have two elements");
//                        }
//                    }).xmap(
//                            ints -> new ChunkPos(ints.get(0), ints.get(1)),
//                            chunkPos -> List.of(chunkPos.x, chunkPos.z)
//                    )
//            )
//            .orElse(Endec.LONG.xmap(ChunkPos::new, ChunkPos::toLong));
//
//    public static final Endec<PacketByteBuf> PACKET_BYTE_BUF = Endec.BYTES
//            .xmap(bytes -> {
//                var buffer = PacketByteBufs.create();
//                buffer.writeBytes(bytes);
//
//                return buffer;
//            }, buffer -> {
//                var bytes = new byte[buffer.readableBytes()];
//                buffer.readBytes(bytes);
//
//                return bytes;
//            });

    // --- Constructors for MC types ---

//    public static <T> Endec<T> ofRegistry(Registry<T> registry) {
//        return IDENTIFIER.xmap(registry::get, registry::getId);
//    }
//
//    public static <T> Endec<TagKey<T>> unprefixedTagKey(RegistryKey<? extends Registry<T>> registry) {
//        return IDENTIFIER.xmap(id -> TagKey.of(registry, id), TagKey::id);
//    }
//
//    public static <T> Endec<TagKey<T>> prefixedTagKey(RegistryKey<? extends Registry<T>> registry) {
//        return Endec.STRING.xmap(
//                s -> TagKey.of(registry, new Identifier(s.substring(1))),
//                tag -> "#" + tag.id()
//        );
//    }

    private static <C, V> Endec<V> vectorEndec(String name, Endec<C> componentEndec, StructEndecBuilder.Function3<C, C, C, V> constructor, Function<V, C> xGetter, Function<V, C> yGetter, Function<V, C> zGetter) {
        return componentEndec.listOf().validate(ints -> {
            if (ints.size() != 3) {
                throw new IllegalStateException(name + " array must have three elements");
            }
        }).xmap(
                components -> constructor.apply(components.get(0), components.get(1), components.get(2)),
                vector -> List.of(xGetter.apply(vector), yGetter.apply(vector), zGetter.apply(vector))
        );
    }
}
