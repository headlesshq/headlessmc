package io.github.headlesshq.headlessmc.platform.purpur;

import io.github.headlesshq.headlessmc.mods.ModType;
import io.github.headlesshq.headlessmc.platform.mods.ModReader;
import io.github.headlesshq.headlessmc.platform.mods.ModSupport;
import io.github.headlesshq.headlessmc.platform.server.ServerFinder;
import io.github.headlesshq.headlessmc.platform.server.ServerInstaller;
import io.github.headlesshq.headlessmc.platform.server.ServerSupport;
import io.github.headlesshq.headlessmc.util.maven.MavenRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import java.util.List;

@ApplicationScoped
public class PurpurFactory {
    @Produces
    @Purpur
    @Dependent
    public MavenRepository getMavenRepository() {
        return MavenRepository.of("https://repo.purpurmc.org/releases");
    }

    @Produces
    @Purpur
    @Dependent
    public ServerSupport createServerSupport(@Purpur ServerFinder finder, @Purpur ServerInstaller installer) {
        return new ServerSupport(finder, installer);
    }

    @Produces
    @Purpur
    @Dependent
    public ModSupport createModSupport(@Purpur Instance<ModReader> modReaders) {
        return new ModSupport(List.of(ModType.PLUGIN), modReaders.stream().toList());
    }

}
