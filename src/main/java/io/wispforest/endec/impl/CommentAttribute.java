package io.wispforest.endec.impl;

import io.wispforest.endec.SerializationAttribute;
import io.wispforest.endec.SerializationAttributes;
import io.wispforest.endec.SerializationContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

///
/// Attribute for holding a comment when encoding a field and the desired comment is meant to be handled by the format
/// if possible to write before the data in the given file
///
public record CommentAttribute(String comment) implements SerializationAttribute.Instance {
    public static final SerializationAttribute.WithValue<CommentAttribute> ATTRIBUTE = SerializationAttribute.withValue("endec_comment");

    @Override
    public SerializationAttribute attribute() {
        return ATTRIBUTE;
    }

    @Override
    public Object value() {
        return this;
    }

    @Nullable
    public static String getComment(SerializationContext ctx) {
        if (ctx.hasAttribute(CommentAttribute.ATTRIBUTE) && !ctx.hasAttribute(SerializationAttributes.DISABLE_COMMENTS)) {
            return ctx.requireAttributeValue(CommentAttribute.ATTRIBUTE).comment();
        }

        return null;
    }

    public static void addComment(SerializationContext ctx, Consumer<String> commentConsumer) {
        var comment = getComment(ctx);

        if (comment != null) commentConsumer.accept(comment);
    }
}
