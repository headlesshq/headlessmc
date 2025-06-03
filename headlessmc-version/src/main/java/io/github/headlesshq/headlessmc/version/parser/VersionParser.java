package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonElement;
import io.github.headlesshq.headlessmc.version.Version;

import java.io.IOException;
import java.nio.file.Path;

public interface VersionParser {
    Version parseVersion(Path file) throws IOException, VersionParseException;

    Version parseVersion(String json) throws VersionParseException;

    Version parseVersion(JsonElement json) throws VersionParseException;

}
