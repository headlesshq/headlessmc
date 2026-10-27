package io.github.headlesshq.headlessmc.mods.github;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.platform.Platform;
import io.github.headlesshq.headlessmc.platform.PlatformService;
import io.github.headlesshq.headlessmc.platform.mods.Mod;
import io.github.headlesshq.headlessmc.platform.mods.ModReader;
import io.github.headlesshq.headlessmc.platform.mods.ModSupport;
import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link ModIdReader} that reads jars with the {@link ModSupport#modReaders()}
 * of the {@link Platform} of the version, e.g. the {@code fabric.mod.json} for Fabric.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PlatformModIdReader implements ModIdReader {
    private final PlatformService platformService;

    @Override
    public Set<String> read(Path jar, VersionArg version) {
        List<ModReader> readers = platformService.getPlatform(version.platform())
            .flatMap(Platform::getModSupport)
            .map(ModSupport::modReaders)
            .orElse(List.of());

        Set<String> result = new HashSet<>();
        for (ModReader reader : readers) {
            try {
                reader.read(jar).stream().map(Mod::id).forEach(result::add);
            } catch (HeadlessMcException e) {
                log.debug("Failed to read mods of {} for {}", jar, version, e);
            } catch (RuntimeException e) {
                log.debug("Failed to parse mods of {} for {}", jar, version, e);
            }
        }

        return result;
    }

}
