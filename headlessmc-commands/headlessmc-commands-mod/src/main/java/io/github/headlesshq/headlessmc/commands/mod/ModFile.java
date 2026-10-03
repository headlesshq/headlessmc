package io.github.headlesshq.headlessmc.commands.mod;

import io.github.headlesshq.headlessmc.mods.ModType;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public record ModFile(
    ModType type,
    Path file,
    String id,
    String name,
    Optional<String> description,
    List<String> authors,
    Optional<String> world
) {
}
