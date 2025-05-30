package io.github.headlesshq.headlessmc.version;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;

public interface Version {
    @Nullable String getParentName();

    String getName();

    int getJava();

    @Nullable String getMainClass();

    Arguments getGameArguments();

    Arguments getJvmArguments();

    @Unmodifiable
    Map<String, Download> getDownloads();

    @Unmodifiable
    List<Library> getLibraries();

    default @Nullable Download getClientDownload() {
        return getDownloads().get("client");
    }

    default @Nullable Download getServerDownload() {
        return getDownloads().get("server");
    }

}
