package io.github.headlesshq.headlessmc.platform.mods;

import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.platform.Platform;

import java.util.List;

/**
 * If a {@link Platform} supports the installation of clients,
 * it needs to implementations for the following classes:
 *
 * @param modTypes   a list of {@link ModType}s that this version handles
 *                   (e.g. {@link ModType#MOD} or {@link ModType#PLUGIN}).
 * @param modReaders a list of ModReaders that allow to list jar files.
 */
public record ModSupport(
    List<ModType> modTypes,
    List<ModReader> modReaders
) {

}
