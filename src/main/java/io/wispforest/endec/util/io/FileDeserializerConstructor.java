package io.wispforest.endec.util.io;

import io.wispforest.endec.Deserializer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface FileDeserializerConstructor<T> {
    Deserializer<T> from(Path path) throws IOException;
}
