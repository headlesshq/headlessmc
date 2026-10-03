package io.github.headlesshq.headlessmc.platform.forge;

import io.github.headlesshq.headlessmc.util.maven.Artifact;
import jakarta.enterprise.context.ApplicationScoped;
import org.jspecify.annotations.Nullable;

@Forge
@ApplicationScoped
public class ForgeArtifactResolverImpl implements ForgeArtifactResolver {
    @Override
    public Artifact resolve(String platformName, PrismIndex index, PrismIndex.Meta meta, @Nullable String classifier) {
        String group = index.uid();
        return new Artifact(group, platformName, getVersion(meta), classifier);
    }

    protected String getVersion(PrismIndex.Meta meta) {
        return meta.getMcVersion() + "-" + meta.version();
    }

}
