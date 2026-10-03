package io.github.headlesshq.headlessmc.platform.purpur;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.mods.Mod;
import io.github.headlesshq.headlessmc.platform.mods.ModReader;
import io.github.headlesshq.headlessmc.platform.paper.Paper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Purpur supports Paper plugins.
 * Thus, we use delegate to the {@link Paper} {@link ModReader}.
 */
@Purpur
@ApplicationScoped
public class PurpurModReader implements ModReader {
    private final ModReader modReader;
    
    @Inject
    public PurpurModReader(@Paper ModReader modReader) {
        this.modReader = modReader;
    }

    @Override
    public List<Mod> read(Path jar) throws HeadlessMcException {
        return modReader.read(jar);
    }

    @Override
    public Optional<List<Mod>> readEntry(InputStream inputStream) throws HeadlessMcException {
        return modReader.readEntry(inputStream);
    }

    @Override
    public Set<String> getEntryNames() {
        return modReader.getEntryNames();
    }
    
}
