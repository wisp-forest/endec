package io.wispforest.endec.util;

import io.wispforest.endec.Serializer;
import io.wispforest.endec.format.edm.EdmSerializer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * A template class for implementing serializers which produce as result an
 * instance of some recursive data structure (like JSON, NBT or EDM)
 * <p>
 * Check {@link EdmSerializer} for a reference implementation
 */
public abstract class RecursiveSerializer<T> implements Serializer<T> {

    protected final Deque<Consumer<T>> frames = new ArrayDeque<>();
    protected T result;

    protected RecursiveSerializer(T initialResult) {
        this.result = initialResult;
        this.frames.push(t -> this.result = t);
    }

    /**
     * Store {@code value} into the current encoding location
     * <p>
     * This location is altered by {@link #frame(FrameAction)} and
     * initially is just the serializer's result directly
     */
    protected void consume(T value) {
        this.frames.peek().accept(value);
    }

    /**
     * Encode the next value down the tree by pushing a new frame
     * onto the encoding stack and invoking {@code action}
     * <p>
     * {@code action} receives {@code encoded}, which is where the next call
     * to {@link #consume(Object)} (which {@code action} must somehow cause) will
     * store the value and allow {@code action} to retrieve it using {@link EncodedValue#value()}
     * or, preferably, {@link EncodedValue#require(String)}
     */
    protected void frame(FrameAction<T> action) {
        var encoded = new EncodedValue<T>();

        this.frames.push(encoded::set);
        action.accept(encoded);
        this.frames.pop();
    }

    @Override
    public T result() {
        return this.result;
    }

    @FunctionalInterface
    protected interface FrameAction<T> {
        void accept(EncodedValue<T> encoded);
    }

    protected static class EncodedValue<T> {
        private T value = null;
        private boolean encoded = false;

        private void set(T value) {
            this.value = value;
            this.encoded = true;
        }

        public T value() {
            return this.value;
        }

        public boolean wasEncoded() {
            return this.encoded;
        }

        public T require(String name) {
            if (!this.encoded) throw new IllegalStateException("Endec for " + name + " serialized nothing");
            return this.value();
        }
    }
}
