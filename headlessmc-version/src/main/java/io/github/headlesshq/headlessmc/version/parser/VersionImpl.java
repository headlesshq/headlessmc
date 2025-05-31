package io.github.headlesshq.headlessmc.version.parser;

import io.github.headlesshq.headlessmc.version.*;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Data
final class VersionImpl implements Version {
    private final @Nullable String parentName;
    private final String name;
    private final @Nullable Integer java;
    private final @Nullable String mainClass;
    private final @Nullable Arguments gameArguments;
    private final @Nullable Arguments jvmArguments;
    private final Map<String, Download> downloads;
    private final List<Library> libraries;
    private final @Nullable Assets assets;
    private final @Nullable Logging logging;

}
