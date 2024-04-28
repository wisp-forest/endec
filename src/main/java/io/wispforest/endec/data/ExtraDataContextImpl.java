package io.wispforest.endec.data;

import java.util.Map;

public record ExtraDataContextImpl(Map<DataToken<?>, Object> tokens) implements ExtraDataContext {

}
