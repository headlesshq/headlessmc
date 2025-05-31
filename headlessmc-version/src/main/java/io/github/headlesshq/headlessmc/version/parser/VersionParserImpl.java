package io.github.headlesshq.headlessmc.version.parser;

import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.version.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
final class VersionParserImpl {
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

}
