package io.wispforest.endec.data;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public sealed interface DataToken<DATA_TYPE> permits DataToken.Marker, DataToken.Instanced {

    Class<DATA_TYPE> clazz();

    String name();

    static Marker marker(String name){
        return new Marker(name);
    }

    static <DATA_TYPE> Instanced<DATA_TYPE> instanced(Class<DATA_TYPE> clazz, String name){
        return new Instanced<>(clazz, name);
    }

    static Map<DataToken<?>, Object> mappedData(Instance... instances) {
        return Arrays.stream(instances).collect(Collectors.toMap(Instance::getToken, Instance::getValue));
    }

    static Stream<Instance> streamedData(Map<DataToken<?>, Object> map) {
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

    record Marker(String name) implements DataToken<Void>, Instance {
        @Override
        public Class<Void> clazz() {
            return Void.class;
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

    record Instanced<DATA_TYPE>(Class<DATA_TYPE> clazz, String name) implements DataToken<DATA_TYPE> {
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

    interface Instance {
        Object getValue();
        DataToken<?> getToken();
    }
}
