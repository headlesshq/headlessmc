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

}
