package io.wispforest.endec;

import io.wispforest.endec.impl.MissingAttributeValueException;
import io.wispforest.endec.impl.trace.EndecTrace;
import io.wispforest.endec.impl.trace.EndecMalformedInputException;

import static io.wispforest.endec.impl.trace.EndecTraceElement.*;

import java.util.*;
import java.util.function.Function;

public final class SerializationContext {

    private static final SerializationContext EMPTY = new SerializationContext(Map.of(), Set.of(), new EndecTrace());

    private final Map<SerializationAttribute, Object> attributeValues;
    private final Set<SerializationAttribute> suppressedAttributes;
    private final EndecTrace trace;

    private SerializationContext(Map<SerializationAttribute, Object> attributeValues, Set<SerializationAttribute> suppressedAttributes, EndecTrace trace) {
        this.attributeValues = Collections.unmodifiableMap(attributeValues);
        this.suppressedAttributes = Collections.unmodifiableSet(suppressedAttributes);
        this.trace = trace;
    }

    //--

    public static SerializationContext empty() {
        return EMPTY;
    }

    public static SerializationContext attributes(SerializationAttribute.Instance... attributes) {
        return (attributes.length == 0)
            ? EMPTY
            : new SerializationContext(unpackAttributes(attributes), Set.of(), new EndecTrace());
    }

    public static SerializationContext suppressed(SerializationAttribute... attributes) {
        return (attributes.length == 0)
            ? EMPTY
            : new SerializationContext(Map.of(), Set.of(attributes), new EndecTrace());
    }

    //--

    public SerializationContext withAttributes(SerializationAttribute.Instance... attributes) {
        var newAttributes = unpackAttributes(attributes);
        this.attributeValues.forEach((attribute, value) -> {
            if (!newAttributes.containsKey(attribute)) {
                newAttributes.put(attribute, value);
            }
        });

        return new SerializationContext(newAttributes, this.suppressedAttributes, this.trace);
    }

    public SerializationContext withoutAttributes(SerializationAttribute... attributes) {
        var newAttributes = new HashMap<>(this.attributeValues);
        for (var attribute : attributes) {
            newAttributes.remove(attribute);
        }

        return new SerializationContext(newAttributes, this.suppressedAttributes, this.trace);
    }

    public SerializationContext withSuppressed(SerializationAttribute... attributes) {
        var newSuppressed = new HashSet<SerializationAttribute>(this.suppressedAttributes);
        newSuppressed.addAll(Arrays.asList(attributes));

        return new SerializationContext(this.attributeValues, newSuppressed, this.trace);
    }

    public SerializationContext withoutSuppressed(SerializationAttribute... attributes) {
        var newSuppressed = new HashSet<>(this.suppressedAttributes);
        for (var attribute : attributes) {
            newSuppressed.remove(attribute);
        }

        return new SerializationContext(this.attributeValues, newSuppressed, this.trace);
    }

    public SerializationContext and(SerializationContext other) {
        if (this.isEmpty()) {
            return other.isEmpty() ? EMPTY : other;
        } else if(other.isEmpty()) {
            return this;
        }

        var newAttributeValues = new HashMap<>(this.attributeValues);
        newAttributeValues.putAll(other.attributeValues);

        var newSuppressed = new HashSet<>(this.suppressedAttributes);
        newSuppressed.addAll(other.suppressedAttributes);

        return new SerializationContext(newAttributeValues, newSuppressed, this.trace);
    }

    public boolean isEmpty() {
        return this.attributeValues.isEmpty() && this.suppressedAttributes.isEmpty();
    }

    //--

    public boolean hasAttribute(SerializationAttribute attribute) {
        return this.attributeValues.containsKey(attribute) && !this.suppressedAttributes.contains(attribute);
    }

    @SuppressWarnings("unchecked")
    public <A> A getAttributeValue(SerializationAttribute.WithValue<A> attribute) {
        return (A) this.attributeValues.get(attribute);
    }

    public <A> A requireAttributeValue(SerializationAttribute.WithValue<A> attribute) {
        if (!this.hasAttribute(attribute)) {
            throw new MissingAttributeValueException("Context did not provide a value for attribute '" + attribute.name + "'");
        }

        return this.getAttributeValue(attribute);
    }

    //--

    public SerializationContext pushField(String fieldName) {
        return new SerializationContext(this.attributeValues, this.suppressedAttributes, this.trace.push(new FieldTraceElement(fieldName)));
    }

    public SerializationContext pushIndex(int index) {
        return new SerializationContext(this.attributeValues, this.suppressedAttributes, this.trace.push(new IndexTraceElement(index)));
    }

    public void throwMalformedInput(String message) throws EndecMalformedInputException {
        throw new EndecMalformedInputException(trace, message);
    }

    public <E extends Exception> E exceptionWithTrace(Function<EndecTrace, E> exceptionFactory) {
        return exceptionFactory.apply(trace);
    }

    //--

    private static Map<SerializationAttribute, Object> unpackAttributes(SerializationAttribute.Instance[] attributes) {
        var attributeValues = new HashMap<SerializationAttribute, Object>();
        for (var instance : attributes) {
            attributeValues.put(instance.attribute(), instance.value());
        }

        return attributeValues;
    }

    //--


    @Override
    public String toString() {
        return "SerializationContext[" +
            "Attributes: " + attributeValues +
            ", SuppressedAttributes: " + suppressedAttributes +
            ", CurrentTrace: " + trace + "]";
    }
}
