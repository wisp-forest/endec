package io.wispforest.endec.data;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract sealed class DataToken<DATA_TYPE> permits DataToken.Marker, DataToken.Instanced {

    protected final Class<DATA_TYPE> clazz;
    protected final String name;

    protected DataToken(Class<DATA_TYPE> clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }

    public static Marker marker(String name){
        return new Marker(name);
    }

    public static <DATA_TYPE> Instanced<DATA_TYPE> instanced(Class<DATA_TYPE> clazz, String name){
        return new Instanced<>(clazz, name);
    }

    public static Map<DataToken<?>, Object> mappedData(Instance... instances) {
        return Arrays.stream(instances).collect(Collectors.toMap(Instance::getToken, Instance::getValue));
    }

    public static Stream<Instance> streamedData(Map<DataToken<?>, Object> map) {
        return map.entrySet().stream()
                .map(entry -> {
                    if(entry.getKey() instanceof DataToken.Instanced<?> instanced) {
                        return instanced.withUnsafe(entry.getValue());
                    } else if(entry.getKey() instanceof DataToken.Marker marker) {
                        return marker;
                    } else {
                        throw new IllegalStateException("Unable to handled the given DataToken as such is not supported to be converted to Instance state! [Token: " + entry.getKey() + "]");
                    }
                });
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return "DataToken[" +
                "clazz=" + clazz + "," +
                "name=" + name + ']';
    }

    public static final class Marker extends DataToken<Void> implements Instance {
        private Marker(String name) {
            super(Void.class, name);
        }

        @Override
        public DataToken<?> getToken() {
            return this;
        }

        @Override
        public Object getValue() {
            return null;
        }
    }

    public static final class Instanced<DATA_TYPE> extends DataToken<DATA_TYPE> {
        public Instanced(Class<DATA_TYPE> clazz, String name) {
            super(clazz, name);
        }

        private Instance withUnsafe(Object data) {
            if(!this.clazz.isInstance(data)) throw new IllegalStateException("Data passed for a given DataToken was found not to be instanceof the token used! [Token: " + this + ", Data: " + data + "]");

            return with((DATA_TYPE) data);
        }

        public Instance with(DATA_TYPE data) {
            return new Instance() {
                @Override
                public Object getValue() {
                    return data;
                }

                @Override
                public DataToken<?> getToken() {
                    return Instanced.this;
                }
            };
        }
    }

    public interface Instance {
        Object getValue();
        DataToken<?> getToken();
    }
}
