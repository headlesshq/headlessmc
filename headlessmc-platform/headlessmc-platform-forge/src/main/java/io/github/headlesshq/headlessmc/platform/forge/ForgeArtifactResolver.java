package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.util.maven.Artifact;
import org.jspecify.annotations.Nullable;

/**
 * Helps with translating a {@link PrismIndex} meta entry into a
 * {@link Artifact} in Forge/NeoForges maven repository.
 * E.g. we have a meta entry for version {@code 48.1.0}
 * for mc {@code 1.20.2}, we can resolve that to an artifact at:
 * {@code net/minecraftforge/forge/1.20.2-48.1.0/forge-1.20.2-48.1.0-installer.jar}.
 * <p>Another example, 1.8.9:
 * <p>{@code net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/forge-1.8.9-11.15.1.2318-1.8.9-installer.jar}
 */
@FunctionalInterface
public interface ForgeArtifactResolver {
    Artifact resolve(String platformName, PrismIndex index, PrismIndex.Meta meta, @Nullable String classifier);

    /**
     * The name the build of an entry in the prism index has in HeadlessMc, the name of the
     * {@link io.github.headlesshq.headlessmc.platform.PlatformVersion} the version service of the platform
     * creates for the entry. It has to be that name, a build with any other name cannot be resolved.
     *
     * @param meta an entry in the prism index.
     * @return the name of the build of the entry.
     */
    default String getBuildName(PrismIndex.Meta meta) {
        return meta.version();
    }

}
