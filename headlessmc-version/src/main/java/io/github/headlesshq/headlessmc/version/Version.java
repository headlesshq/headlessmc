package io.github.headlesshq.headlessmc.version;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public interface Version {
    @Nullable String getParentName();

    String getName();

    int getJava();

    @Nullable Download getClientDownload();

    @Nullable Download getServerDownload();

    @Nullable String getMainClass();

    @Unmodifiable
    List<Library> getLibraries();

    @Unmodifiable
    List<Argument> getGameArguments();

    @Unmodifiable
    List<Argument> getJvmArguments();

}
