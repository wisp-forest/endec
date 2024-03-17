package io.wispforest.endec;

import java.util.function.Function;

public abstract class DataToken<DATA_TYPE> {

    public static final DataToken<Void> SELF_DESCRIBING = create(Void.class, "self_describing");
    public static final DataToken<Void> HUMAN_READABLE = create(Void.class, "human_readable");

    private final Class<DATA_TYPE> clazz;
    private final String name;

    private DataToken(Class<DATA_TYPE> clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }

    public static <DATA_TYPE> DataToken<DATA_TYPE> create(Class<DATA_TYPE> clazz, String name){
        return new DataToken<>(clazz, name) {};
    }

    public DataTokenHolder<DATA_TYPE> holderFrom(DATA_TYPE data){
        return new DataTokenHolder<>(this, data);
    }

    public DataTokenHolder<DATA_TYPE> holderFromUnsafe(Object data){
        if(!clazz.isInstance(data)) throw new IllegalStateException("Data passed for a given DataToken was found not to be instanceof the token used! [Token: " + this + "]");

        return new DataTokenHolder<>(this, (DATA_TYPE) data);
    }

    public Function<ExtraDataContext, DATA_TYPE> getter(){
        return context -> context.getOrThrow(this);
    }

    public <V> Function<ExtraDataContext, V> buildFrom(Function<DATA_TYPE, V> function){
        return context -> function.apply(context.getOrThrow(this));
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return "DataReferenceKey[" +
                "clazz=" + clazz + "," +
                "name=" + name + ']';
    }

}
