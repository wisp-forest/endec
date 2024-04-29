package io.wispforest.endec.impl;

import com.mojang.datafixers.util.Either;
import io.wispforest.endec.*;
import io.wispforest.endec.data.SerializationContext;

public final class EitherEndec <L, R> implements Endec<Either<L, R>> {

    private final Endec<L> leftEndec;
    private final Endec<R> rightEndec;

    private final boolean exclusive;

    public EitherEndec(Endec<L> leftEndec, Endec<R> rightEndec, boolean exclusive) {
        this.leftEndec = leftEndec;
        this.rightEndec = rightEndec;

        this.exclusive = exclusive;
    }

    @Override
    public void encode(SerializationContext ctx, Serializer<?> serializer, Either<L, R> either) {
        if (serializer instanceof SelfDescribedSerializer<?>) {
            either.ifLeft(left -> this.leftEndec.encode(ctx, serializer, left))
                    .ifRight(right -> this.rightEndec.encode(ctx, serializer, right));
        } else {
            either.ifLeft(left -> {
                try (var struct = serializer.struct()) {
                    struct.field(ctx, "is_left", Endec.BOOLEAN, true).field(ctx, "left", this.leftEndec, left);
                }
            }).ifRight(right -> {
                try (var struct = serializer.struct()) {
                    struct.field(ctx, "is_left", Endec.BOOLEAN, false).field(ctx, "right", this.rightEndec, right);
                }
            });
        }
    }

    @Override
    public Either<L, R> decode(SerializationContext ctx, Deserializer<?> deserializer) {
        if (deserializer instanceof SelfDescribedDeserializer<?>) {
            Either<L, R> leftResult = null;
            try {
                leftResult = Either.left(deserializer.tryRead(ctx, this.leftEndec::decode));
            } catch (Exception ignore) {}

            if (!this.exclusive && leftResult != null) return leftResult;

            Either<L, R> rightResult = null;
            try {
                rightResult = Either.right(deserializer.tryRead(ctx, this.rightEndec::decode));
            } catch (Exception ignore) {}

            if (this.exclusive && leftResult != null && rightResult != null) {
                throw new IllegalStateException("Both alternatives read successfully, can not pick the correct one; first: " + leftResult + " second: " + rightResult);
            }

            if (leftResult != null) return leftResult;
            if (rightResult != null) return rightResult;

            throw new IllegalStateException("Neither alternative read successfully");
        } else {
            var struct = deserializer.struct();

            return (struct.field(ctx, "is_left", Endec.BOOLEAN))
                ? Either.left(struct.field(ctx, "left", this.leftEndec))
                : Either.right(struct.field(ctx, "right", this.rightEndec));
        }

    }
}
