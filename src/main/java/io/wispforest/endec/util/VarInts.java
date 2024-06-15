package io.wispforest.endec.util;

import it.unimi.dsi.fastutil.bytes.ByteConsumer;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class VarInts {

    public static final int SEGMENT_BITS = 127;
    public static final int CONTINUE_BIT = 128;

    public static int getSizeInBytesFromInt(int i){
        for(int j = 1; j < 5; ++j) {
            if ((i & -1 << j * 7) == 0) {
                return j;
            }
        }

        return 5;
    }

    public static int getSizeInBytesFromLong(long l) {
        for(int i = 1; i < 10; ++i) {
            if ((l & -1L << i * 7) == 0L) {
                return i;
            }
        }

        return 10;
    }

    public static int readInt(ByteSupplier readByteSup) {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = readByteSup.get();
            value |= (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) break;

            position += 7;

            if (position >= 32) throw new RuntimeException("VarInt is too big");
        }

        return value;
    }

    public static void writeInt(int value, ByteConsumer writeByteFunc) {
        while (true) {
            if ((value & ~SEGMENT_BITS) == 0) {
                writeByteFunc.accept((byte) value);
                return;
            }

            writeByteFunc.accept((byte) ((value & SEGMENT_BITS) | CONTINUE_BIT));

            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value >>>= 7;
        }
    }

    public static long readLong(ByteSupplier readByteSup) {
        long value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = readByteSup.get();
            value |= (long) (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) break;

            position += 7;

            if (position >= 64) throw new RuntimeException("VarLong is too big");
        }

        return value;
    }

    public static void writeLong(long value, ByteConsumer writeByteFunc) {
        while (true) {
            if ((value & ~((long) SEGMENT_BITS)) == 0) {
                writeByteFunc.accept((byte) value);
                return;
            }

            writeByteFunc.accept((byte) ((value & SEGMENT_BITS) | CONTINUE_BIT));

            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value >>>= 7;
        }
    }

    public interface ByteSupplier {
        byte get();
    }
}
