package io.wispforest.endec.format.file;

import io.wispforest.endec.Serializer;
import io.wispforest.endec.format.forwarding.ForwardingSerializer;
import io.wispforest.endec.util.io.Object2ByteArray;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileWriter<T> extends ForwardingSerializer<T> {

    public Path path;

    public Object2ByteArray<T> func;

    public FileWriter(Path path, Serializer<T> serializer, Object2ByteArray<T> func) {
        super(serializer);

        this.path = path;
        this.func = func;
    }

    public void writeResult() throws IOException {
        Files.write(path, func.from(this.delegate().result()));
    }
}

