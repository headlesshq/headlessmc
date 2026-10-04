package io.github.headlesshq.headlessmc.auth.store.gson;

import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.Lock;

public class GsonFileAuthStore<T> extends GsonAuthStore<T> {
    protected final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    // TODO: make dynamic
    protected final Path file;

    public GsonFileAuthStore(GsonCodec<T> codec, Lock lock, long version, Path file) {
        super(codec, lock, version);
        this.file = file;
    }

    @Override
    protected JsonObject readJson() throws IOException {
        if (!Files.exists(file)) {
            return new JsonObject();
        }

        try (
            InputStream inputStream = Files.newInputStream(file);
            InputStreamReader reader = new InputStreamReader(inputStream)
        ) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }

            return new JsonObject();
        } catch (JsonParseException e) { // TODO: could this leak secrets?
            throw new IOException("Failed to read accounts json file " + file, e);
        }
    }

    @Override
    protected void writeJson(JsonElement object) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String fileContents = gson.toJson(object);
        // maybe be even more safe, and write to temp file first?
        try (OutputStream outputStream = Files.newOutputStream(file)) {
            outputStream.write(fileContents.getBytes(StandardCharsets.UTF_8));
        } catch (JsonIOException e) { // TODO: could this leak secrets?
            throw new IOException("Failed to read accounts json file " + file, e);
        }
    }

}
