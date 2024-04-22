package io.wispforest.endec.format;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.wispforest.endec.format.file.FileWriter;
import io.wispforest.endec.format.json.JsonDeserializer;
import io.wispforest.endec.format.json.JsonSerializer;
import io.wispforest.endec.util.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;

public class JsonIo {

    private static final Gson GSON = new GsonBuilder()
            .setLenient()
            .setPrettyPrinting()
            .create();

    public static JsonDeserializer fileReader(Path path) throws IOException {
        return FileUtils.stringFileReader(path, JsonDeserializer::of, (s) -> GSON.fromJson(s, JsonElement.class));
    }

    public static FileWriter<JsonElement> fileWriter(Path path) {
        return FileUtils.stringFileWriter(path, JsonSerializer::of, GSON::toJson);
    }
}
