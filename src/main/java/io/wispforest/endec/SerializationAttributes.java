package io.wispforest.endec;

public class SerializationAttributes {

    /**
     * This format is intended to be human-readable (and potentially -editable)
     * <p>
     * Endecs should use this to make decisions like representing a
     * {@link net.minecraft.util.math.BlockPos} as an integer sequence instead of packing it into a long
     */
    public static final SerializationAttribute.Marker HUMAN_READABLE = SerializationAttribute.marker("human_readable");
    public static final SerializationAttribute.Marker DISABLE_COMMENTS = SerializationAttribute.marker("disable_comments");
}
