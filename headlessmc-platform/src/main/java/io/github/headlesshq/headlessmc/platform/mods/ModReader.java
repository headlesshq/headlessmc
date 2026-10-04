package io.github.headlesshq.headlessmc.platform.mods;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads {@link Mod} information from (jar) files.
 */
public interface ModReader {
    List<Mod> read(Path jar) throws HeadlessMcException;

    Optional<List<Mod>> readEntry(InputStream inputStream) throws HeadlessMcException;

    Set<String> getEntryNames();

    // TODO: eventually get mod logo/image, think of API, should be on demand
    //  e.g. put in mod, get entries?

}
