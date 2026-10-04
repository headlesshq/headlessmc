package io.github.headlesshq.headlessmc.platform.paper;

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
public class PaperFactory {
    @Produces
    @Paper
    @Dependent
    public MavenRepository getMavenRepository() {
        return MavenRepository.of("https://repo.papermc.io/repository/maven-public");
    }

    @Produces
    @Paper
    @Dependent
    public ServerSupport createServerSupport(@Paper ServerFinder finder, @Paper ServerInstaller installer) {
        return new ServerSupport(finder, installer);
    }

    @Produces
    @Paper
    @Dependent
    public ModSupport createModSupport(@Paper Instance<ModReader> modReaders) {
        return new ModSupport(List.of(ModType.PLUGIN), modReaders.stream().toList());
    }

}
