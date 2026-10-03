package io.github.headlesshq.headlessmc.mods.distribution;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;

import java.nio.file.Path;
import java.util.Optional;

public interface ModCache {
    Optional<RemoteMod> find(Path file) throws HeadlessMcIOException;

    void add(Path file, RemoteMod mod);

}
