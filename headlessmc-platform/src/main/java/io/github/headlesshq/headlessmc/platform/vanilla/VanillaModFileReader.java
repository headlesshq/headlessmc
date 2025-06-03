package io.github.headlesshq.headlessmc.platform.vanilla;

import io.github.headlesshq.headlessmc.platform.Mod;
import io.github.headlesshq.headlessmc.platform.ModFileReader;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Vanilla does not support mods. This {@link ModFileReader} always throws an IOException.
 */
@Vanilla
@ApplicationScoped
final class VanillaModFileReader implements ModFileReader {
    @Override
    public List<Mod> read(Path file) throws IOException {
        throw new IOException("Vanilla does not support mods.");
    }

}
