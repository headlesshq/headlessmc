package io.github.headlesshq.headlessmc.platform;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.file.Path;
import java.util.List;

public interface Mod {
    String getName();

    Path getPath();

    @Unmodifiable
    List<String> getAuthors();

    @Nullable
    String getDescription();

}
