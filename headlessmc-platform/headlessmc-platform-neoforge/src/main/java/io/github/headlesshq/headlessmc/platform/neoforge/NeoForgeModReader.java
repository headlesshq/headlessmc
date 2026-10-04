package io.github.headlesshq.headlessmc.platform.neoforge;

import io.github.headlesshq.headlessmc.platform.forge.ForgeModsTomlReader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.toml.TomlMapper;

@NeoForge
@ApplicationScoped
public class NeoForgeModReader extends ForgeModsTomlReader {
    // TODO: NeoForge only supports forge.mods.toml up to a certain version (1.21.0 or something I think)
    //  we should detect that, also if forge.mods.toml is accepted mods might appear twice,
    //  or support both forge/neoforge, we should keep that in mind
    @Inject
    public NeoForgeModReader() {
        this("META-INF/neoforge.mods.toml", "META-INF/forge.mods.toml");
    }

    public NeoForgeModReader(String... entries) {
        super(TomlMapper.builder().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY).build(), entries);
    }

    @Override
    protected boolean stopAfterFoundEntry() {
        // if we found a neoforge.mods.toml, don't continue
        // might be a mod which contains files for both platforms
        return true;
    }

}
