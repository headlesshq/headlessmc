package io.github.headlesshq.headlessmc.platform;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

@Data
public class ModImpl implements Mod {
    private final String name;
    private final Path path;
    private final List<String> authors;
    private final @Nullable String description;

}
