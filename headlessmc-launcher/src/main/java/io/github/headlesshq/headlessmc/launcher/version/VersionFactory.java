package io.github.headlesshq.headlessmc.launcher.version;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

// TODO: check if we could make this easier using @SerializedName
@RequiredArgsConstructor
class VersionFactory {
    private final LibraryFactory libraryFactory;
    private final JavaMajorVersionParser javaParser;
    private final ArgumentFactory argumentFactory;

    public Version parse(JsonObject json, File folder, Supplier<Integer> id)
            throws VersionParseException {
        val parentName = JsonUtil.getString(json, "inheritsFrom");
        val assets = JsonUtil.getString(json, "assets");
        val mainClass = JsonUtil.getString(json, "mainClass");
        val name = JsonUtil.getString(json, "id");
        val libraries = libraryFactory.parseLibraries(json.get("libraries"));
        val majorVersion = javaParser.parse(json.get("javaVersion"));
        JsonElement argumentElement = json.get("arguments");
        val type = JsonUtil.getString(json, "type");
        if (argumentElement == null) {
            argumentElement = json.get("minecraftArguments");
        }

        val assetsUrl = JsonUtil.getString(json, "assetIndex", "url");

        val clientUrl = JsonUtil.getString(json, "downloads", "client", "url");
        val clientSha1 = JsonUtil.getString(json, "downloads", "client", "sha1");
        val clientSize = JsonUtil.getLong(json, "downloads", "client", "size");
        VersionExecutable clientDownload = clientUrl == null
                ? null
                : new VersionExecutable(clientUrl, clientSha1, clientSize);

        val serverUrl = JsonUtil.getString(json, "downloads", "server", "url");
        val serverSha1 = JsonUtil.getString(json, "downloads", "server", "sha1");
        val serverSize = JsonUtil.getLong(json, "downloads", "server", "size");
        VersionExecutable serverDownload = serverUrl == null
                ? null
                : new VersionExecutable(serverUrl, serverSha1, serverSize);

        val logging = Logging.getFromVersion(json);
        val newFormat = new AtomicBoolean();
        val arguments = argumentFactory.parse(argumentElement, newFormat::set);
        return VersionImpl
                .builder()
                .folder(folder)
                .json(json)
                .type(type == null ? "unknown" : type)
                .arguments(arguments)
                .java(majorVersion)
                .id(id.get())
                .assets(assets)
                .assetsUrl(assetsUrl)
                .name(name)
                .parentName(parentName)
                .mainClass(mainClass)
                .newArgumentFormat(newFormat.get())
                .libraries(libraries)
                .clientDownload(clientDownload)
                .serverDownload(serverDownload)
                .logging(logging)
                .build();
    }

}
