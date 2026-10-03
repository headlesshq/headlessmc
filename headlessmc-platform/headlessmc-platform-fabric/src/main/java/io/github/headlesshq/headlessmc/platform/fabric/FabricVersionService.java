package io.github.headlesshq.headlessmc.platform.fabric;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import io.github.headlesshq.headlessmc.files.cache.Cache;
import io.github.headlesshq.headlessmc.platform.PlatformVersion;
import io.github.headlesshq.headlessmc.platform.VanillaVersion;
import io.github.headlesshq.headlessmc.platform.util.AbstractJsonVersionService;
import io.quarkus.runtime.annotations.RegisterResources;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

@Slf4j
@RegisterResources(globs = "fabric/fabric-versions.json")
final class FabricVersionService extends AbstractJsonVersionService<PlatformVersion, List<BuildData>> {
    public FabricVersionService(Cache<List<BuildData>> cache) {
        super(Fabric.PLATFORM_NAME, cache);
    }

    @Override
    public SequencedSet<PlatformVersion> getBuilds(VanillaVersion version) throws HeadlessMcException {
        // on fabric builds are just loader versions,
        // and we generally assume that all loaders can load all versions
        // this might not be true, but we don't care about old fabric versions that much.
        return getVersions();
    }

    @Override
    protected SequencedSet<PlatformVersion> map(
        List<BuildData> value,
        @Nullable SequencedSet<PlatformVersion> previous
    ) {
        SequencedSet<PlatformVersion> result = new LinkedHashSet<>();
        for (BuildData buildData : value) {
            result.add(createVersion(buildData.version()));
        }

        return verify(value, result, previous);
    }

}
