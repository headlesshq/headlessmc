package io.github.headlesshq.headlessmc.platform.neoforge;

import io.github.headlesshq.headlessmc.platform.forge.Forge;
import io.github.headlesshq.headlessmc.platform.forge.ForgeArtifactResolver;
import io.github.headlesshq.headlessmc.platform.forge.PrismIndex;
import io.github.headlesshq.headlessmc.util.maven.Artifact;
import jakarta.enterprise.context.ApplicationScoped;
import org.jspecify.annotations.Nullable;

/**
 * In the NeoForge maven repository there is an exception for artifacts
 * for mc-version {@code 1.20.1}. This version released when NeoForge was still
 * freshly forked and uses the forge as the artifact name, e.g.:
 * <p>{@code net/neoforged/forge/1.20.1-47.1.104/forge-1.20.1-47.1.104-installer.jar}
 * <p>Newer artifacts use {@code neoforge} as name and also the Neoforge version without mc-version.
 * <p>{@code net/neoforged/neoforge/20.4.195/neoforge-20.4.195-installer.jar}
 */
@NeoForge
@ApplicationScoped
public class NeoForgeArtifactResolver implements ForgeArtifactResolver {
    @Override
    public Artifact resolve(String platformName, PrismIndex index, PrismIndex.Meta meta, @Nullable String classifier) {
        String mcVersion = meta.getMcVersion();
        if ("1.20.1".equals(mcVersion)) {
            return new Artifact(index.uid(), Forge.PLATFORM_NAME, mcVersion + "-" + meta.version(), classifier);
        }

        return new Artifact(index.uid(), platformName, meta.version(), classifier);
    }

}
