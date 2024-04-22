package io.wispforest.endec.format;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.api.SyntaxError;
import io.wispforest.endec.format.file.FileWriter;
import io.wispforest.endec.format.json.JsonDeserializer;
import io.wispforest.endec.format.json.JsonSerializer;
import io.wispforest.endec.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class JsonIo {

    private static final Jankson INSTANCE = new Jankson.Builder()
            .build();

    public static JsonDeserializer fileReader(Path path) throws IOException {
        return FileUtils.stringFileReader(path, JsonDeserializer::of, (s) -> {
            try {
                return INSTANCE.fromJson(s, JsonElement.class);
            } catch (SyntaxError e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static FileWriter<JsonElement> fileWriter(Path path) {
        return FileUtils.stringFileWriter(path, JsonSerializer::of, JsonElement::toString);
    }
}
