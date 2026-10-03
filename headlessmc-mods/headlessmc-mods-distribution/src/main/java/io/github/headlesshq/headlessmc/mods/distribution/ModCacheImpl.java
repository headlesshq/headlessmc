package io.github.headlesshq.headlessmc.mods.distribution;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Path;
import java.util.Optional;

// TODO: implement this
@ApplicationScoped
public class ModCacheImpl implements ModCache {
    @Override
    public Optional<RemoteMod> find(Path file) throws HeadlessMcIOException {
        return Optional.empty();
    }

    @Override
    public void add(Path file, RemoteMod mod) {

    }

}
