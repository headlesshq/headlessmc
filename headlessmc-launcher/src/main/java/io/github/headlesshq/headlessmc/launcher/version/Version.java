package io.github.headlesshq.headlessmc.launcher.version;

import com.google.gson.JsonObject;
import io.github.headlesshq.headlessmc.api.traits.HasId;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.launcher.version.family.HasParent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * Represents a Minecraft version.
 * More specifically a parsed version.json of a Minecraft version.
 */
// TODO: support logging.xml file?
public interface Version extends HasName, HasId, HasParent<Version> {
    File getFolder();

    JsonObject getJson();

    int getId();

    @Nullable String getParentName();

    String getAssets();

    String getType();

    String getAssetsUrl();

    Integer getJava();

    String getMainClass();

    List<Library> getLibraries();

    List<Argument> getArguments();

    boolean isNewArgumentFormat();

    @Nullable VersionExecutable getClientDownload();

    @Nullable VersionExecutable getServerDownload();

    @Nullable Logging getLogging();

}
