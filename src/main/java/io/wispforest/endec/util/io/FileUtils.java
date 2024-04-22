package io.wispforest.endec.util.io;

import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.format.file.FileWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

public class FileUtils {

    public static <T> FileWriter<T> fileWriter(Path path, Supplier<Serializer<T>> serializer, Object2ByteArray<T> func) {
        return new FileWriter<>(path, serializer.get(), func);
    }

    public static <T> FileWriter<T> stringFileWriter(Path path, Supplier<Serializer<T>> serializer, Function<T, String> func) {
        return new FileWriter<>(path, serializer.get(), func.andThen(String::getBytes)::apply);
    }

    public static <T, D extends Deserializer<T>> D fileReader(Path path, Function<T, D> deserializer, ByteArray2Object<T> func) throws IOException {
        return deserializer.apply(func.from(Files.readAllBytes(path)));
    }

    public static <T, D extends Deserializer<T>> D stringFileReader(Path path, Function<T, D> deserializer, Function<String, T> func) throws IOException {
        return fileReader(path, deserializer, ByteArray2Object.fromString(func));
    }

    //--

    public static <T> T decode(Path path, FileDeserializerConstructor<?> deserializerConstructor, Endec<T> endec) throws IOException {
        return endec.decode(deserializerConstructor.from(path));
    }

    public static <T, E> void encode(Path path, FileSerializerConstructor<E> serializerConstructor, Endec<T> endec, T t) throws IOException {
        var serializer = serializerConstructor.from(path);

        endec.encode(serializer, t);

        serializer.writeResult();
    }
}
