package io.wispforest.endec.util.io;

import io.wispforest.endec.format.file.FileWriter;

import java.io.File;
import java.nio.file.Path;

public interface FileSerializerConstructor<T> {
    FileWriter<T> from(Path path);
}
