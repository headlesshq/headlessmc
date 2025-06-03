package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.headlesshq.headlessmc.version.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
final class VersionParserImpl implements VersionParser {
    private final DownloadsParser downloadsParser;
    private final ArgumentParser argumentParser;
    private final LibraryParser libraryParser;
    private final AssetsParser assetsParser;
    private final JavaParser javaParser;

    public Version parse(JsonObject json) throws VersionParseException {
        String name = JsonUtil.getString(json, "id");
        if (name == null) {
            throw new VersionParseException("Failed to get id of version: " + json);
        }

        String parentName = JsonUtil.getString(json, "inheritsFrom");
        String mainClass = JsonUtil.getString(json, "mainClass");
        List<Library> libraries = libraryParser.parseLibraries(json.get("libraries"));
        Integer javaVersion = javaParser.parse(json.get("javaVersion"));
        Map<String, Download> downloads = downloadsParser.parseDownloads(json);
        Assets assets = assetsParser.parse(json);
        Logging logging = LoggingImpl.getFromVersion(json);
        ArgumentParseResult arguments = argumentParser.parse(json);
        return new VersionImpl(
            parentName,
            name,
            javaVersion,
            mainClass,
            arguments.getGameArguments(),
            arguments.getJvmArguments(),
            downloads,
            libraries,
            assets,
            logging
        );
    }

    @Override
    public Version parseVersion(Path file) throws IOException, VersionParseException {
        String json = new String(Files.readAllBytes(file));
        return parseVersion(json);
    }

    @Override
    public Version parseVersion(String json) throws VersionParseException {
        try {
            JsonElement element = JsonParser.parseString(json);
            return parseVersion(element);
        } catch (Exception e) {
            throw e instanceof VersionParseException ? (VersionParseException) e : new VersionParseException(e);
        }
    }

    @Override
    public Version parseVersion(JsonElement json) throws VersionParseException {
        try {
            JsonObject object = json.getAsJsonObject();
            return parse(object);
        } catch (Exception e) {
            throw e instanceof VersionParseException ? (VersionParseException) e : new VersionParseException(e);
        }
    }

}
