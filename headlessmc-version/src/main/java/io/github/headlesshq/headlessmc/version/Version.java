package io.github.headlesshq.headlessmc.version;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;

public interface Version {
    @Nullable String getParentName();

    String getName();

    @Nullable Integer getJava();

    @Nullable String getMainClass();

    @Nullable Arguments getGameArguments();

    @Nullable Arguments getJvmArguments();

    @Unmodifiable
    Map<String, Download> getDownloads();

    @Unmodifiable
    List<Library> getLibraries();

    @Nullable Assets getAssets();

    @Nullable Logging getLogging();

    default @Nullable Download getClientDownload() {
        return getDownloads().get("client");
    }

    default @Nullable Download getServerDownload() {
        return getDownloads().get("server");
    }

}
