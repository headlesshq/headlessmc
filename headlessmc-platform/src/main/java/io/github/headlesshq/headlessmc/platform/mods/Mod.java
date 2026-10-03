package io.github.headlesshq.headlessmc.platform.mods;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a (Minecraft) mod.
 *
 * @param id           the unique id of the mod.
 * @param name         the human-readable name of the mod.
 * @param authors      the authors of the mod, possibly empty.
 * @param description  the description of the mod, optional.
 * @param dependencies dependencies of the mod.
 */
public record Mod(
    String id,
    String name,
    Optional<String> description,
    List<String> authors,
    Map<String, List<String>> dependencies
) {

}
