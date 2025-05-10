package io.wispforest.endec;

import java.lang.annotation.*;
import java.util.function.IntFunction;

public class ReflectionDebugging {

    @Nothing
    private static GenericArrayClass<String> obj4 = new GenericArrayClass<>();

    private static @Nothing GenericArrayClass<String> obj5 = new GenericArrayClass<>(String[]::new);

    private static GenericArrayClass<@Nothing String> obj6 = new GenericArrayClass<>(String[]::new);

    private static String[] obj7 = new String[0];

    private static GenericArrayClass<? extends String> obj8 = new GenericArrayClass<>(String[]::new);

    public static void main(String[] args) {
        GenericArrayClass<String> obj1 = new GenericArrayClass<>();
        GenericArrayClass<String> obj2 = new GenericArrayClass<>(String[]::new);
        GenericArrayClass<String> obj3 = new GenericArrayClass<>("test");

        System.out.println(obj1.getClass());
        System.out.println(obj2.getClass());
        System.out.println(obj3.getClass());
        System.out.println(obj4.getClass());
        System.out.println(obj5.getClass());
        System.out.println(obj6.getClass());

        System.out.println("--------");

        var fields = ReflectionDebugging.class.getDeclaredFields();

        for (var field : fields) {
            System.out.println(field);
        }

        System.out.println("All done!");
    }

    public static class GenericArrayClass<T> {
        public T[] array = (T[]) new Object[0];

        public GenericArrayClass() {}

        public GenericArrayClass(IntFunction<T[]> arrayConstructor) {
            this.array = arrayConstructor.apply(0);
        }

        public GenericArrayClass(T baseValue) {
            array = (T[]) new Object[] {baseValue};
        }

        public T get(int index) {
            return this.array[index];
        }
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    public @interface Nothing {}
}
