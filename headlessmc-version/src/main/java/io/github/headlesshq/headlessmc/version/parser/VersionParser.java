package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.headlesshq.headlessmc.version.Version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VersionParser {
    public Version parseVersion(Path file) throws IOException, VersionParseException {
        String json = new String(Files.readAllBytes(file));
        return parseVersion(json);
    }

    public Version parseVersion(String json) throws VersionParseException {
        try {
            JsonElement element = JsonParser.parseString(json);
            return parseVersion(element);
        } catch (Exception e) {
            throw e instanceof VersionParseException ? (VersionParseException) e : new VersionParseException(e);
        }
    }

    public Version parseVersion(JsonElement json) throws VersionParseException {
        try {
            JsonObject object = json.getAsJsonObject();
            return null;
        } catch (Exception e) {
            throw e instanceof VersionParseException ? (VersionParseException) e : new VersionParseException(e);
        }
    }

}
